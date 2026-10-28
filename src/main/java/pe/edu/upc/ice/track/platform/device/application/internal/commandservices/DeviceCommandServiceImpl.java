package pe.edu.upc.ice.track.platform.device.application.internal.commandservices;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.device.application.commandservices.DeviceCommandService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.DeviceExternalOwnerService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.assetmanagement.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.DeviceCredentialService;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceCredentialResult;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.ChangeDeviceStatusCommand;
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
  private final DeviceExternalOwnerService deviceExternalOwnerService;
  private final DeviceCredentialService deviceCredentialService;

  public DeviceCommandServiceImpl(
      DeviceRepository deviceRepository,
      ExternalAssetManagementService externalAssetManagementService,
      DeviceExternalOwnerService deviceExternalOwnerService,
      DeviceCredentialService deviceCredentialService) {
    this.deviceRepository = deviceRepository;
    this.externalAssetManagementService = externalAssetManagementService;
    this.deviceExternalOwnerService = deviceExternalOwnerService;
    this.deviceCredentialService = deviceCredentialService;
  }

  @Override
  @Transactional
  public Result<DeviceCredentialResult, ApplicationError> handle(RegisterDeviceCommand command) {
    return resolveOwnerId(command.username()).flatMap(ownerId -> {
      if (!externalAssetManagementService.existsEquipmentForOwner(command.equipmentId(), ownerId)) {
        return Result.failure(ApplicationError.notFound("Equipment", String.valueOf(command.equipmentId())));
      }
      if (deviceRepository.existsByDeviceUid(command.deviceUid())) {
        return Result.failure(ApplicationError.conflict(
            DEVICE_RESOURCE, "A device with uid '%s' already exists".formatted(command.deviceUid())));
      }
      try {
        // The first save assigns the persistence identity required by the registration and pairing
        // domain events. Both writes remain in this transaction.
        var device = deviceRepository.save(new Device(
            null, command.deviceUid(), command.boardModel(), command.firmwareVersion()));
        var credential = deviceCredentialService.issue();
        device.replaceApiKeyHash(credential.hash());
        device.pairTo(command.equipmentId());
        return Result.success(new DeviceCredentialResult(
            deviceRepository.save(device), credential.plainText()));
      } catch (IllegalArgumentException e) {
        return Result.failure(ApplicationError.validationError(DEVICE_RESOURCE, e.getMessage()));
      } catch (IllegalStateException e) {
        return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, e.getMessage()));
      } catch (DataIntegrityViolationException e) {
        return Result.failure(ApplicationError.conflict(
            DEVICE_RESOURCE, "A device with uid '%s' already exists".formatted(command.deviceUid())));
      }
    });
  }

  @Override
  @Transactional
  public Result<Device, ApplicationError> handle(UnpairDeviceCommand command) {
    return ownedPairedDevice(command.deviceId(), command.username()).flatMap(device -> {
      try {
        device.unpair();
        return Result.success(deviceRepository.save(device));
      } catch (IllegalStateException e) {
        return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, e.getMessage()));
      }
    });
  }

  @Override
  @Transactional
  public Result<DeviceCredentialResult, ApplicationError> handle(RotateApiKeyCommand command) {
    return ownedPairedDevice(command.deviceId(), command.username()).flatMap(device -> {
      try {
        var credential = deviceCredentialService.issue();
        device.replaceApiKeyHash(credential.hash());
        return Result.success(new DeviceCredentialResult(deviceRepository.save(device), credential.plainText()));
      } catch (IllegalStateException e) {
        return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, e.getMessage()));
      }
    });
  }

  @Override
  @Transactional
  public Result<Device, ApplicationError> handle(ChangeDeviceStatusCommand command) {
    return ownedPairedDevice(command.deviceId(), command.username()).flatMap(device -> {
      try {
        device.changeStatus(command.newStatus());
        return Result.success(deviceRepository.save(device));
      } catch (IllegalArgumentException e) {
        return Result.failure(ApplicationError.validationError(DEVICE_RESOURCE, e.getMessage()));
      } catch (IllegalStateException e) {
        return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, e.getMessage()));
      }
    });
  }

  @Override
  @Transactional
  public void recordActivity(Long deviceId, LocalDateTime seenAt) {
    deviceRepository.findById(deviceId).ifPresent(device -> {
      device.recordActivity(seenAt);
      deviceRepository.save(device);
    });
  }

  /** Resolves the caller username to the owner profile it represents. */
  private Result<Long, ApplicationError> resolveOwnerId(String username) {
    return deviceExternalOwnerService.fetchOwnerIdByUsername(username)
        .<Result<Long, ApplicationError>>map(Result::success)
        .orElseGet(() -> Result.failure(ApplicationError.accessDenied(
            "The authenticated account has no owner profile, so it owns no device")));
  }

  /** Resolves a board only when it is paired to equipment owned by the requesting username. */
  private Result<Device, ApplicationError> ownedPairedDevice(Long deviceId, String username) {
    return resolveOwnerId(username).flatMap(ownerId -> deviceRepository.findById(deviceId)
        .filter(device -> device.getPairedEquipmentId() != null)
        .filter(device -> externalAssetManagementService.existsEquipmentForOwner(
            device.getPairedEquipmentId(), ownerId))
        .<Result<Device, ApplicationError>>map(Result::success)
        .orElseGet(() -> Result.failure(
            ApplicationError.notFound(DEVICE_RESOURCE, String.valueOf(deviceId)))));
  }
}
