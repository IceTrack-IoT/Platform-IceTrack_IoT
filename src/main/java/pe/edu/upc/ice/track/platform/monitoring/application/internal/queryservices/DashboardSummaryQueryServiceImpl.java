package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalDeviceServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalProfilesServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalServiceRequestServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.MonitoredEquipment;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.DashboardSummaryQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetDashboardSummaryQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.readmodels.DashboardSummary;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.readmodels.DashboardSummary.AlertFigures;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.readmodels.DashboardSummary.DeviceFigures;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.readmodels.DashboardSummary.EquipmentFigures;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.readmodels.DashboardSummary.ServiceRequestFigures;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link DashboardSummaryQueryService}.
 *
 * <p>The summary is composed from what each context publishes through its ACL facade - the units and
 * sites of Assets Management, the requests of Service Request Management, the boards of Device
 * Management - and from the alerts and policies this context owns. The units of the caller are read
 * first: Monitoring and Device Management key their data by unit alone, so those units are what
 * scopes every other figure to the caller.</p>
 *
 * <p>The number of statements does not depend on how many units the owner has: each figure is one
 * read over all of them, never one per unit.</p>
 */
@Service
public class DashboardSummaryQueryServiceImpl implements DashboardSummaryQueryService {

  private static final String SITE_RESOURCE = "Site";

  private static final List<AlertStatus> ACTIVE_STATUSES = List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

  private final AlertRepository alertRepository;
  private final AlertPolicyRepository alertPolicyRepository;
  private final ExternalProfilesServiceForMonitoring externalProfilesService;
  private final ExternalAssetManagementService externalAssetManagementService;
  private final ExternalServiceRequestServiceForMonitoring externalServiceRequestService;
  private final ExternalDeviceServiceForMonitoring externalDeviceService;

  public DashboardSummaryQueryServiceImpl(
      AlertRepository alertRepository,
      AlertPolicyRepository alertPolicyRepository,
      ExternalProfilesServiceForMonitoring externalProfilesService,
      ExternalAssetManagementService externalAssetManagementService,
      ExternalServiceRequestServiceForMonitoring externalServiceRequestService,
      ExternalDeviceServiceForMonitoring externalDeviceService) {
    this.alertRepository = alertRepository;
    this.alertPolicyRepository = alertPolicyRepository;
    this.externalProfilesService = externalProfilesService;
    this.externalAssetManagementService = externalAssetManagementService;
    this.externalServiceRequestService = externalServiceRequestService;
    this.externalDeviceService = externalDeviceService;
  }

  /**
   * {@inheritDoc}
   *
   * <p>A site filter is checked against the caller before anything is counted, and a site that does
   * not exist answers exactly like one that belongs to another owner. A caller that holds no owner
   * profile owns nothing, hence a summary of zeros.</p>
   */
  @Override
  @Transactional(readOnly = true)
  public Result<DashboardSummary, ApplicationError> handle(GetDashboardSummaryQuery query) {
    var ownerId = externalProfilesService.fetchOwnerIdByUsername(query.username()).orElse(null);
    if (query.siteId() != null && !externalAssetManagementService.isSiteOwnedBy(query.siteId(), ownerId)) {
      return Result.failure(ApplicationError.notFound(SITE_RESOURCE, String.valueOf(query.siteId())));
    }
    var generatedAt = Instant.now();
    var equipment = externalAssetManagementService.fetchEquipmentOwnedBy(ownerId, query.siteId());
    var equipmentIds = equipment.stream().map(MonitoredEquipment::equipmentId).toList();
    var siteCount = query.siteId() != null ? 1 : externalAssetManagementService.countSitesOwnedBy(ownerId);
    return Result.success(new DashboardSummary(
        generatedAt,
        siteCount,
        equipmentFigures(equipment, equipmentIds, generatedAt),
        alertFigures(equipmentIds),
        serviceRequestFigures(ownerId, query.siteId()),
        deviceFigures(equipmentIds)));
  }

  /**
   * Counts the units per status and splits them by whether they are reporting.
   *
   * <p>A unit is silent once its latest reading is older than the offline window of the policy that
   * applies to it - its own, or the platform default - which is the rule offline detection applies.
   * Judging it here, at {@code now} by the server clock, is what spares every client from comparing
   * a reading instant with its own clock.</p>
   *
   * @param equipment    the units in scope
   * @param equipmentIds their identifiers
   * @param now          the instant to judge at
   * @return the equipment figures
   */
  private EquipmentFigures equipmentFigures(List<MonitoredEquipment> equipment, List<Long> equipmentIds, Instant now) {
    var byStatus = new LinkedHashMap<String, Long>();
    externalAssetManagementService.fetchEquipmentStatusNames().forEach(status -> byStatus.put(status, 0L));
    var ownPolicies = alertPolicyRepository.findByEquipmentIdIn(equipmentIds).stream()
        .collect(Collectors.toMap(
            policy -> policy.getEquipmentId().equipmentId(), Function.identity(), (first, second) -> first));
    var defaultPolicy = alertPolicyRepository.findDefaultPolicy().orElseGet(AlertPolicy::platformDefault);
    var reporting = 0L;
    var silent = 0L;
    var neverReported = 0L;
    for (var unit : equipment) {
      byStatus.merge(unit.status(), 1L, Long::sum);
      if (unit.lastReadingAt() == null) {
        neverReported++;
      } else if (ownPolicies.getOrDefault(unit.equipmentId(), defaultPolicy).isOfflineSince(unit.lastReadingAt(), now)) {
        silent++;
      } else {
        reporting++;
      }
    }
    return new EquipmentFigures(equipment.size(), byStatus, reporting, silent, neverReported);
  }

  /**
   * Counts the active alerts of the units in scope, per status and per severity.
   *
   * @param equipmentIds identifiers of the units in scope
   * @return the alert figures, every severity present
   */
  private AlertFigures alertFigures(List<Long> equipmentIds) {
    var activeBySeverity = new EnumMap<AlertSeverity, Long>(AlertSeverity.class);
    for (var severity : AlertSeverity.values()) {
      activeBySeverity.put(severity, 0L);
    }
    var open = 0L;
    var acknowledged = 0L;
    for (var alert : alertRepository.findByEquipmentIdInAndStatusIn(equipmentIds, ACTIVE_STATUSES)) {
      activeBySeverity.merge(alert.getSeverity(), 1L, Long::sum);
      if (alert.getStatus() == AlertStatus.OPEN) {
        open++;
      } else {
        acknowledged++;
      }
    }
    return new AlertFigures(open, acknowledged, activeBySeverity);
  }

  private ServiceRequestFigures serviceRequestFigures(Long ownerId, Long siteId) {
    var totals = externalServiceRequestService.countServiceRequests(ownerId, siteId);
    return new ServiceRequestFigures(totals.active(), totals.byStatus());
  }

  private DeviceFigures deviceFigures(List<Long> equipmentIds) {
    var totals = externalDeviceService.countLinkedDevices(equipmentIds);
    return new DeviceFigures(totals.paired(), totals.revoked());
  }
}
