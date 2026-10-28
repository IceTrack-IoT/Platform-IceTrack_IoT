package pe.edu.upc.ice.track.platform.device.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.DeviceExternalOwnerService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.assetmanagement.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.DeviceCredentialService;
import pe.edu.upc.ice.track.platform.device.application.queryservices.DeviceQueryService;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceAuthenticationResult;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDeviceByIdQuery;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDevicesByEquipmentQuery;
import pe.edu.upc.ice.track.platform.device.domain.repositories.DeviceRepository;

import java.util.List;
import java.util.Optional;

/** Default implementation of the device read and authentication use cases. */
@Service
public class DeviceQueryServiceImpl implements DeviceQueryService {

  private final DeviceRepository deviceRepository;
  private final ExternalAssetManagementService externalAssetManagementService;
  private final DeviceExternalOwnerService deviceExternalOwnerService;
  private final DeviceCredentialService deviceCredentialService;

  public DeviceQueryServiceImpl(
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
  public Optional<Device> handle(GetDeviceByIdQuery query) {
    return deviceRepository.findById(query.deviceId())
        .filter(device -> deviceExternalOwnerService.fetchOwnerIdByUsername(query.username())
            .filter(ownerId -> isOwnedBy(device, ownerId))
            .isPresent());
  }

  @Override
  public List<Device> handle(GetDevicesByEquipmentQuery query) {
    var ownerId = deviceExternalOwnerService.fetchOwnerIdByUsername(query.username()).orElse(null);
    if (!externalAssetManagementService.existsEquipmentForOwner(query.equipmentId(), ownerId)) {
      return List.of();
    }
    return deviceRepository.findByEquipmentId(query.equipmentId());
  }

  @Override
  public Optional<DeviceAuthenticationResult> authenticate(String deviceUid, String apiKey) {
    if (deviceUid == null || deviceUid.isBlank() || apiKey == null || apiKey.isBlank()) {
      return Optional.empty();
    }
    return deviceRepository.findByDeviceUid(deviceUid)
        .filter(Device::isActiveAndPaired)
        .filter(device -> deviceCredentialService.matches(apiKey, device.getApiKeyHash()))
        .map(device -> new DeviceAuthenticationResult(device.getDeviceId(), device.getPairedEquipmentId()));
  }

  private boolean isOwnedBy(Device device, Long ownerId) {
    return device.getPairedEquipmentId() != null
        && externalAssetManagementService.existsEquipmentForOwner(device.getPairedEquipmentId(), ownerId);
  }
}
