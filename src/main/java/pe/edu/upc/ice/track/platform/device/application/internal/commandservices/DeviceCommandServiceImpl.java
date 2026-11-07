package pe.edu.upc.ice.track.platform.device.application.internal.commandservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.device.application.commandservices.DeviceCommandService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.DeviceExternalOwnerService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.assetmanagement.ExternalAssetManagementServiceForDevice;
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

/**
 * Default implementation of the device lifecycle use cases.
 *
 * <p>Pairing answers, in this order: 404 when the equipment does not exist or belongs to someone
 * else; 409 "Equipment already has a paired device" when a board (paired or revoked) is already
 * linked to that equipment; 409 "Device already paired" when the board's UID is linked to any
 * equipment. The second message never echoes the UID, the equipment or its owner. A board that
 * was unpaired keeps its row and is paired again with a fresh key, so its UID is never burned.</p>
 *
 * <p>A unique-index violation raised while saving is not caught here: it has already marked the
 * transaction rollback-only, so it is left to the global handler, which answers 409.</p>
 */
@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

  private static final String DEVICE_RESOURCE = "Device";
  private static final String EQUIPMENT_RESOURCE = "Equipment";
  static final String DEVICE_ALREADY_PAIRED = "Device already paired";
  static final String EQUIPMENT_ALREADY_PAIRED = "Equipment already has a paired device";

  private final DeviceRepository deviceRepository;
  private final ExternalAssetManagementServiceForDevice externalAssetManagementService;
  private final DeviceExternalOwnerService deviceExternalOwnerService;
  private final DeviceCredentialService deviceCredentialService;

  public DeviceCommandServiceImpl(
      DeviceRepository deviceRepository,
      ExternalAssetManagementServiceForDevice externalAssetManagementService,
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
        return Result.failure(ApplicationError.notFound(EQUIPMENT_RESOURCE, String.valueOf(command.equipmentId())));
      }
      if (deviceRepository.existsLinkedToEquipment(command.equipmentId())) {
        return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, EQUIPMENT_ALREADY_PAIRED));
      }
      var existing = deviceRepository.findByDeviceUid(command.deviceUid());
      if (existing.isPresent() && existing.get().isLinkedToEquipment()) {
        return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, DEVICE_ALREADY_PAIRED));
      }
      try {
        final Device device;
        if (existing.isPresent()) {
          // An unpaired board is paired again: same row and UID, fresh credential.
          device = existing.get();
          device.describeHardware(command.boardModel(), command.firmwareVersion());
        } else {
          // The first save assigns the persistence identity the registration and pairing events
          // carry. Both writes stay in this transaction.
          device = deviceRepository.save(new Device(
              null, command.deviceUid(), command.boardModel(), command.firmwareVersion()));
        }
        var credential = deviceCredentialService.issue();
        device.pairTo(command.equipmentId(), credential.digest());
        return Result.success(new DeviceCredentialResult(deviceRepository.save(device), credential.plainText()));
      } catch (IllegalArgumentException e) {
        return Result.failure(ApplicationError.validationError(DEVICE_RESOURCE, e.getMessage()));
      } catch (IllegalStateException e) {
        return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, DEVICE_ALREADY_PAIRED));
      }
    });
  }

  @Override
  @Transactional
  public Result<Device, ApplicationError> handle(UnpairDeviceCommand command) {
    return ownedLinkedDevice(command.deviceId(), command.username()).flatMap(device -> {
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
    return ownedLinkedDevice(command.deviceId(), command.username()).flatMap(device -> {
      try {
        var credential = deviceCredentialService.issue();
        device.rotateCredential(credential.digest());
        return Result.success(new DeviceCredentialResult(deviceRepository.save(device), credential.plainText()));
      } catch (IllegalStateException e) {
        return Result.failure(ApplicationError.conflict(DEVICE_RESOURCE, e.getMessage()));
      }
    });
  }

  @Override
  @Transactional
  public Result<Device, ApplicationError> handle(ChangeDeviceStatusCommand command) {
    return ownedLinkedDevice(command.deviceId(), command.username()).flatMap(device -> {
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
    deviceRepository.findById(deviceId).filter(Device::isPaired).ifPresent(device -> {
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

  /**
   * Resolves a board only when it is linked (paired or revoked) to equipment owned by the
   * requesting username. Anything else answers 404, so a caller learns nothing about other
   * owners' boards.
   */
  private Result<Device, ApplicationError> ownedLinkedDevice(Long deviceId, String username) {
    return resolveOwnerId(username).flatMap(ownerId -> deviceRepository.findById(deviceId)
        .filter(Device::isLinkedToEquipment)
        .filter(device -> externalAssetManagementService.existsEquipmentForOwner(
            device.getPairedEquipmentId(), ownerId))
        .<Result<Device, ApplicationError>>map(Result::success)
        .orElseGet(() -> Result.failure(
            ApplicationError.notFound(DEVICE_RESOURCE, String.valueOf(deviceId)))));
  }
}
