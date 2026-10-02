package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

/**
 * Query to fetch the alert policy applicable to an equipment.
 *
 * <p>Resolution falls back to the platform-wide default policy when no
 * equipment-specific policy is active.</p>
 *
 * @param equipmentId identifier of the equipment
 */
public record GetAlertPolicyByEquipmentQuery(Long equipmentId) {
}
