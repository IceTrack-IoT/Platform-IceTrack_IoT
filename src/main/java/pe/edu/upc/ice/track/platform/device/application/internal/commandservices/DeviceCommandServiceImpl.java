package pe.edu.upc.ice.track.platform.device.application.internal.commandservices;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.device.application.commandservices.DeviceCommandService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.assetmanagement.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.DeviceCredentialService;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceCredentialResult;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.ChangeDeviceStatusCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.PairDeviceCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.RegisterDeviceCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.RotateApiKeyCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.UnpairDeviceCommand;
import pe.edu.upc.ice.track.platform.device.domain.repositories.DeviceRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.time.LocalDateTime;

/** Default implementation of the device lifecycle use cases. */
@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

  private static final String DEVICE_RESOURCE = "Device";

  private final DeviceRepository deviceRepository;
  private final ExternalAssetManagementService externalAssetManagementService;
  private final DeviceCredentialService deviceCredentialService;

  public DeviceCommandServiceImpl(
      DeviceRepository deviceRepository,
      ExternalAssetManagementService externalAssetManagementService,
      DeviceCredentialService deviceCredentialService) {
    this.deviceRepository = deviceRepository;
    this.externalAssetManagementService = externalAssetManagementService;
    this.deviceCredentialService = deviceCredentialService;
  }

  @Override
  @Transactional
  public Result<Device, ApplicationError> handle(RegisterDeviceCommand command) {
    if (deviceRepository.existsByDeviceUid(command.deviceUid())) {
      return Result.failure(ApplicationError.conflict(
          DEVICE_RESOURCE, "A device with uid '%s' already exists".formatted(command.deviceUid())));
    }
    try {
      var saved = deviceRepository.save(new Device(
          null, command.deviceUid(), command.boardModel(), command.firmwareVersion()));
      return Result.success(saved);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(DEVICE_RESOURCE, e.getMessage()));
    } catch (DataIntegrityViolationException e) {
      return Result.failure(ApplicationError.conflict(
          DEVICE_RESOURCE, "A device with uid '%s' already exists".formatted(command.deviceUid())));
    }
  }

  @Override
  @Transactional
  public Result<DeviceCredentialResult, ApplicationError> handle(PairDeviceCommand command) {
    if (!externalAssetManagementService.existsEquipmentForOwner(command.equipmentId(), command.ownerId())) {
      return Result.failure(ApplicationError.notFound("Equipment", String.valueOf(command.equipmentId())));
    }
    var device = deviceRepository.findById(command.deviceId());
    if (device.isEmpty()) {
      return Result.failure(ApplicationError.notFound(DEVICE_RESOURCE, String.valueOf(command.deviceId())));
    }
    if (device.get().getPairedEquipmentId() != null) {
      return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, "Device is already paired with an equipment"));
    }
    try {
      var credential = deviceCredentialService.issue();
      device.get().replaceApiKeyHash(credential.hash());
      device.get().pairTo(command.equipmentId());
      return Result.success(new DeviceCredentialResult(
          deviceRepository.save(device.get()), credential.plainText()));
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(DEVICE_RESOURCE, e.getMessage()));
    } catch (IllegalStateException e) {
      return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, e.getMessage()));
    }
  }

  @Override
  @Transactional
  public Result<Device, ApplicationError> handle(UnpairDeviceCommand command) {
    var device = ownedPairedDevice(command.deviceId(), command.ownerId());
    if (device == null) {
      return Result.failure(ApplicationError.notFound(DEVICE_RESOURCE, String.valueOf(command.deviceId())));
    }
    try {
      device.unpair();
      return Result.success(deviceRepository.save(device));
    } catch (IllegalStateException e) {
      return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, e.getMessage()));
    }
  }

  @Override
  @Transactional
  public Result<DeviceCredentialResult, ApplicationError> handle(RotateApiKeyCommand command) {
    var device = ownedPairedDevice(command.deviceId(), command.ownerId());
    if (device == null) {
      return Result.failure(ApplicationError.notFound(DEVICE_RESOURCE, String.valueOf(command.deviceId())));
    }
    try {
      var credential = deviceCredentialService.issue();
      device.replaceApiKeyHash(credential.hash());
      return Result.success(new DeviceCredentialResult(deviceRepository.save(device), credential.plainText()));
    } catch (IllegalStateException e) {
      return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, e.getMessage()));
    }
  }

  @Override
  @Transactional
  public Result<Device, ApplicationError> handle(ChangeDeviceStatusCommand command) {
    var device = ownedPairedDevice(command.deviceId(), command.ownerId());
    if (device == null) {
      return Result.failure(ApplicationError.notFound(DEVICE_RESOURCE, String.valueOf(command.deviceId())));
    }
    try {
      device.changeStatus(command.newStatus());
      return Result.success(deviceRepository.save(device));
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(DEVICE_RESOURCE, e.getMessage()));
    } catch (IllegalStateException e) {
      return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, e.getMessage()));
    }
  }

  @Override
  @Transactional
  public void recordActivity(Long deviceId, LocalDateTime seenAt) {
    deviceRepository.findById(deviceId).ifPresent(device -> {
      device.recordActivity(seenAt);
      deviceRepository.save(device);
    });
  }

  /** Resolves a board only when it is currently paired to equipment owned by the caller. */
  private Device ownedPairedDevice(Long deviceId, Long ownerId) {
    return deviceRepository.findById(deviceId)
        .filter(device -> device.getPairedEquipmentId() != null)
        .filter(device -> externalAssetManagementService.existsEquipmentForOwner(
            device.getPairedEquipmentId(), ownerId))
        .orElse(null);
  }
}
