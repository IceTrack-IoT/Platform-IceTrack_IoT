package pe.edu.upc.ice.track.platform.device.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.DeviceExternalOwnerService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalAssetManagementServiceForDevice;
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
  private final ExternalAssetManagementServiceForDevice externalAssetManagementService;
  private final DeviceExternalOwnerService deviceExternalOwnerService;
  private final DeviceCredentialService deviceCredentialService;

  public DeviceQueryServiceImpl(
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

  /**
   * {@inheritDoc}
   *
   * <p>The key is digested and the board found through the unique index on its digest. The
   * constant-time comparison afterwards adds nothing the indexed equality did not already check,
   * but it keeps the verification explicit and independent of how the repository matches. The
   * device is re-read on every call, so a revocation or unpairing takes effect on the very next
   * request.</p>
   */
  @Override
  public Optional<DeviceAuthenticationResult> authenticateByApiKey(String apiKey) {
    return deviceCredentialService.digestOf(apiKey)
        .flatMap(deviceRepository::findByApiKeyDigest)
        .filter(Device::isPaired)
        .filter(device -> deviceCredentialService.matches(apiKey, device.getApiKeyDigest()))
        .map(device -> new DeviceAuthenticationResult(device.getDeviceId(), device.getPairedEquipmentId()));
  }

  private boolean isOwnedBy(Device device, Long ownerId) {
    return device.getPairedEquipmentId() != null
        && externalAssetManagementService.existsEquipmentForOwner(device.getPairedEquipmentId(), ownerId);
  }
}
