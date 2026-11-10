package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalProfilesServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.MonitoredEquipment;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertPolicyQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertPolicyByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetApplicableAlertPoliciesQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ApplicableAlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link AlertPolicyQueryService}, scoped by {@link EquipmentAccessPolicy}.
 *
 * <p>The batch read is scoped by the units of the caller instead, read once through the outbound ACL
 * service: the units asked for are intersected with them, so a unit of another owner simply has no
 * entry and nothing tells it apart from a unit that does not exist.</p>
 */
@Service
public class AlertPolicyQueryServiceImpl implements AlertPolicyQueryService {

  private final AlertPolicyRepository alertPolicyRepository;
  private final EquipmentAccessPolicy equipmentAccessPolicy;
  private final ExternalProfilesServiceForMonitoring externalProfilesService;
  private final ExternalAssetManagementService externalAssetManagementService;

  public AlertPolicyQueryServiceImpl(
      AlertPolicyRepository alertPolicyRepository,
      EquipmentAccessPolicy equipmentAccessPolicy,
      ExternalProfilesServiceForMonitoring externalProfilesService,
      ExternalAssetManagementService externalAssetManagementService) {
    this.alertPolicyRepository = alertPolicyRepository;
    this.equipmentAccessPolicy = equipmentAccessPolicy;
    this.externalProfilesService = externalProfilesService;
    this.externalAssetManagementService = externalAssetManagementService;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AlertPolicy> handle(GetAlertPolicyByEquipmentQuery query) {
    if (!equipmentAccessPolicy.canRead(query.equipmentId(), query.username(), query.callerRole())) {
      return Optional.empty();
    }
    return alertPolicyRepository.findApplicableTo(query.equipmentId());
  }

  /**
   * {@inheritDoc}
   *
   * <p>Three reads whatever the number of units: the units of the caller, the policies those units
   * have of their own, and the platform default every other unit falls back to. When no default is
   * stored the built-in one applies, as it does when a reading is evaluated.</p>
   */
  @Override
  @Transactional(readOnly = true)
  public List<ApplicableAlertPolicy> handle(GetApplicableAlertPoliciesQuery query) {
    var ownerId = externalProfilesService.fetchOwnerIdByUsername(query.username()).orElse(null);
    var ownedEquipmentIds = externalAssetManagementService.fetchEquipmentOwnedBy(ownerId, null).stream()
        .map(MonitoredEquipment::equipmentId)
        .collect(Collectors.toSet());
    var equipmentIds = scopeOf(query, ownedEquipmentIds);
    if (equipmentIds.isEmpty()) {
      return List.of();
    }
    var ownPolicies = alertPolicyRepository.findByEquipmentIdIn(equipmentIds).stream()
        .collect(Collectors.toMap(
            policy -> policy.getEquipmentId().equipmentId(), Function.identity(), (first, second) -> first));
    var defaultPolicy = alertPolicyRepository.findDefaultPolicy().orElseGet(AlertPolicy::platformDefault);
    return equipmentIds.stream()
        .map(equipmentId -> new ApplicableAlertPolicy(
            equipmentId, ownPolicies.getOrDefault(equipmentId, defaultPolicy)))
        .toList();
  }

  /**
   * Resolves the units the answer covers: every unit of the caller, or those among the units asked
   * for that the caller owns.
   *
   * @param query             the query
   * @param ownedEquipmentIds the units of the caller
   * @return the units to answer, by ascending identifier
   */
  private static List<Long> scopeOf(GetApplicableAlertPoliciesQuery query, Set<Long> ownedEquipmentIds) {
    Collection<Long> candidates = query.equipmentIds().isEmpty() ? ownedEquipmentIds : query.equipmentIds();
    return candidates.stream().filter(ownedEquipmentIds::contains).sorted().toList();
  }
}
