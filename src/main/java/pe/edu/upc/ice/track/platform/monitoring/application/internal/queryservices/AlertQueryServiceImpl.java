package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalProfilesServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.MonitoredEquipment;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertByIdQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertsQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetOpenAlertsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertPage;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link AlertQueryService}.
 *
 * <p>Every read is scoped by {@link EquipmentAccessPolicy}: an alert of a unit the caller may not
 * read is answered exactly like a missing one.</p>
 *
 * <p>The owner-wide listing is scoped differently, since it names no unit to check: the units of the
 * caller are read once through the outbound ACL service and their identifiers bound the query, so
 * the alerts of every unit come back in a single statement instead of one request per unit.</p>
 */
@Service
public class AlertQueryServiceImpl implements AlertQueryService {

  private static final String SITE_RESOURCE = "Site";
  private static final String EQUIPMENT_RESOURCE = "Equipment";

  private final AlertRepository alertRepository;
  private final EquipmentAccessPolicy equipmentAccessPolicy;
  private final ExternalProfilesServiceForMonitoring externalProfilesService;
  private final ExternalAssetManagementService externalAssetManagementService;

  public AlertQueryServiceImpl(
      AlertRepository alertRepository,
      EquipmentAccessPolicy equipmentAccessPolicy,
      ExternalProfilesServiceForMonitoring externalProfilesService,
      ExternalAssetManagementService externalAssetManagementService) {
    this.alertRepository = alertRepository;
    this.equipmentAccessPolicy = equipmentAccessPolicy;
    this.externalProfilesService = externalProfilesService;
    this.externalAssetManagementService = externalAssetManagementService;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Alert> handle(GetAlertByIdQuery query) {
    return alertRepository.findById(query.alertId())
        .filter(alert -> equipmentAccessPolicy.canRead(alert.getEquipmentId(), query.username(), query.callerRole()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Alert> handle(GetOpenAlertsByEquipmentQuery query) {
    if (!equipmentAccessPolicy.canRead(query.equipmentId(), query.username(), query.callerRole())) {
      return List.of();
    }
    return alertRepository.findByEquipmentIdAndStatusIn(
        query.equipmentId(), List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED));
  }

  /**
   * {@inheritDoc}
   *
   * <p>A site or unit filter is checked against the caller before anything is listed, and a filter
   * that does not exist answers exactly like one that belongs to another owner. A caller that holds
   * no owner profile owns no unit, hence an empty page.</p>
   */
  @Override
  @Transactional(readOnly = true)
  public Result<AlertPage, ApplicationError> handle(GetAlertsQuery query) {
    var ownerId = externalProfilesService.fetchOwnerIdByUsername(query.username()).orElse(null);
    if (query.siteId() != null && !externalAssetManagementService.isSiteOwnedBy(query.siteId(), ownerId)) {
      return Result.failure(ApplicationError.notFound(SITE_RESOURCE, String.valueOf(query.siteId())));
    }
    if (query.equipmentId() != null
        && !externalAssetManagementService.isEquipmentOwnedBy(query.equipmentId(), ownerId)) {
      return Result.failure(ApplicationError.notFound(EQUIPMENT_RESOURCE, String.valueOf(query.equipmentId())));
    }
    var ownedEquipmentIds = externalAssetManagementService.fetchEquipmentOwnedBy(ownerId, query.siteId()).stream()
        .map(MonitoredEquipment::equipmentId)
        .collect(Collectors.toSet());
    return Result.success(alertRepository.findPageByEquipmentIdIn(
        scopeOf(query, ownedEquipmentIds), query.statuses(), query.types(), query.severities(),
        query.openedFrom(), query.openedTo(), query.page(), query.size()));
  }

  /**
   * Narrows the units of the caller to the one the query asks for, when it asks for one.
   *
   * @param query             the query
   * @param ownedEquipmentIds the units of the caller, already narrowed to the site of the query
   * @return the units whose alerts are listed; empty when the unit asked for is not among them, as
   *         when it is installed at another site than the one asked for
   */
  private static Set<Long> scopeOf(GetAlertsQuery query, Set<Long> ownedEquipmentIds) {
    if (query.equipmentId() == null) {
      return ownedEquipmentIds;
    }
    return ownedEquipmentIds.contains(query.equipmentId()) ? Set.of(query.equipmentId()) : Set.of();
  }
}
