package pe.edu.upc.ice.track.platform.device.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.DeviceExternalOwnerService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalAssetManagementServiceForDevice;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalAssetManagementServiceForDevice.PairedEquipment;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.DeviceCredentialService;
import pe.edu.upc.ice.track.platform.device.application.queryservices.DeviceQueryService;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceAuthenticationResult;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceDetails;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceDetailsPage;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDeviceByIdQuery;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDevicesQuery;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DevicePage;
import pe.edu.upc.ice.track.platform.device.domain.repositories.DeviceRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default implementation of the device read and authentication use cases.
 *
 * <p>A board carries no owner: it belongs to whoever owns the unit it is linked to, and units live
 * in Assets Management. The inventory is therefore answered in two steps. The units of the caller
 * are read once through the outbound ACL service, then the boards linked to those units are read in
 * a single statement. The same units give each board the name and the site it is shown with, so no
 * request is made per board.</p>
 */
@Service
public class DeviceQueryServiceImpl implements DeviceQueryService {

  private static final String SITE_RESOURCE = "Site";
  private static final String EQUIPMENT_RESOURCE = "Equipment";

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
  public Optional<DeviceDetails> handle(GetDeviceByIdQuery query) {
    return deviceRepository.findById(query.deviceId())
        .filter(device -> deviceExternalOwnerService.fetchOwnerIdByUsername(query.username())
            .filter(ownerId -> isOwnedBy(device, ownerId))
            .isPresent())
        .map(this::describe);
  }

  /**
   * {@inheritDoc}
   *
   * <p>A site or unit filter is checked against the caller before anything is listed, and a filter
   * that does not exist answers exactly like one that belongs to another owner. A caller that holds
   * no owner profile owns no unit, hence an empty inventory.</p>
   */
  @Override
  public Result<DeviceDetailsPage, ApplicationError> handle(GetDevicesQuery query) {
    var ownerId = deviceExternalOwnerService.fetchOwnerIdByUsername(query.username()).orElse(null);
    if (query.siteId() != null && !externalAssetManagementService.existsSiteForOwner(query.siteId(), ownerId)) {
      return Result.failure(ApplicationError.notFound(SITE_RESOURCE, String.valueOf(query.siteId())));
    }
    if (query.equipmentId() != null
        && !externalAssetManagementService.existsEquipmentForOwner(query.equipmentId(), ownerId)) {
      return Result.failure(ApplicationError.notFound(EQUIPMENT_RESOURCE, String.valueOf(query.equipmentId())));
    }
    var ownedEquipment = externalAssetManagementService.fetchEquipmentOwnedBy(ownerId, query.siteId()).stream()
        .collect(Collectors.toMap(PairedEquipment::equipmentId, Function.identity()));
    var scope = scopeOf(query, ownedEquipment.keySet());
    var page = query.isPaged()
        ? deviceRepository.findPageLinkedToEquipment(scope, query.statuses(), query.search(), query.page(), query.size())
        : DevicePage.unpaged(deviceRepository.findAllLinkedToEquipment(scope, query.statuses(), query.search()));
    var content = page.content().stream()
        .map(device -> toDetails(device, ownedEquipment.get(device.getPairedEquipmentId())))
        .toList();
    return Result.success(new DeviceDetailsPage(content, page.page(), page.size(), page.totalElements()));
  }

  // inherited javadoc
  @Override
  public DeviceDetails describe(Device device) {
    return toDetails(
        device, externalAssetManagementService.fetchPairedEquipment(device.getPairedEquipmentId()).orElse(null));
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

  /**
   * Narrows the units of the caller to the one the query asks for, when it asks for one.
   *
   * @param query             the query
   * @param ownedEquipmentIds the units of the caller, already narrowed to the site of the query
   * @return the units whose boards are listed; empty when the unit asked for is not among them, as
   *         when it is installed at another site than the one asked for
   */
  private static Set<Long> scopeOf(GetDevicesQuery query, Set<Long> ownedEquipmentIds) {
    if (query.equipmentId() == null) {
      return ownedEquipmentIds;
    }
    return ownedEquipmentIds.contains(query.equipmentId()) ? Set.of(query.equipmentId()) : Set.of();
  }

  private static DeviceDetails toDetails(Device device, PairedEquipment equipment) {
    return equipment == null
        ? DeviceDetails.withoutEquipment(device)
        : new DeviceDetails(device, equipment.name(), equipment.siteId());
  }

  private boolean isOwnedBy(Device device, Long ownerId) {
    return device.getPairedEquipmentId() != null
        && externalAssetManagementService.existsEquipmentForOwner(device.getPairedEquipmentId(), ownerId);
  }
}
