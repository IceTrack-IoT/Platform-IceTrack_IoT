package pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalProfilesServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalServiceRequestServiceForMonitoring;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

/**
 * Decides whether an authenticated caller may read or manage the monitoring data of a unit.
 *
 * <p>Monitoring owns no notion of tenancy: telemetry, alerts and per-unit policies are keyed by
 * equipment alone. Tenancy lives in Asset Management (a unit belongs to whoever owns its site) and
 * work assignments live in Service Request Management, so this policy combines the two through the
 * outbound ACL services:</p>
 * <ul>
 *   <li><strong>Read</strong> - the owner of the unit, or a technician holding an active
 *       assignment on it.</li>
 *   <li><strong>Manage</strong> - the owner of the unit only (alert lifecycle, alert-policy changes).</li>
 * </ul>
 *
 * <p>Every query and command service of this context that is reachable from REST asks this policy
 * before touching data, and reports a refusal as "not found", so identifiers of other tenants
 * cannot be harvested.</p>
 */
@Service
public class EquipmentAccessPolicy {

  private final ExternalAssetManagementService externalAssetManagementService;
  private final ExternalProfilesServiceForMonitoring externalProfilesService;
  private final ExternalServiceRequestServiceForMonitoring externalServiceRequestService;

  public EquipmentAccessPolicy(
      ExternalAssetManagementService externalAssetManagementService,
      ExternalProfilesServiceForMonitoring externalProfilesService,
      ExternalServiceRequestServiceForMonitoring externalServiceRequestService) {
    this.externalAssetManagementService = externalAssetManagementService;
    this.externalProfilesService = externalProfilesService;
    this.externalServiceRequestService = externalServiceRequestService;
  }

  /**
   * @param equipmentId identifier of the unit
   * @param username    username of the authenticated caller
   * @param callerRole  the role the caller acts in; {@code null} is treated as
   *                    {@link CallerRole#OWNER}
   * @return {@code true} when the caller owns the unit, or is a technician actively assigned to it
   */
  public boolean canRead(Long equipmentId, String username, CallerRole callerRole) {
    if (equipmentId == null) {
      return false;
    }
    if (callerRole == CallerRole.TECHNICIAN) {
      return externalProfilesService.fetchTechnicianIdByUsername(username)
          .map(technicianId -> externalServiceRequestService.hasActiveAssignmentOnEquipment(technicianId, equipmentId))
          .orElse(false);
    }
    return isOwner(equipmentId, username);
  }

  /**
   * @param equipmentId identifier of the unit
   * @param username    username of the authenticated caller
   * @return {@code true} only when the caller holds an owner profile that owns the unit
   */
  public boolean canManage(Long equipmentId, String username) {
    return equipmentId != null && isOwner(equipmentId, username);
  }

  private boolean isOwner(Long equipmentId, String username) {
    return externalProfilesService.fetchOwnerIdByUsername(username)
        .map(ownerId -> externalAssetManagementService.isEquipmentOwnedBy(equipmentId, ownerId))
        .orElse(false);
  }
}
