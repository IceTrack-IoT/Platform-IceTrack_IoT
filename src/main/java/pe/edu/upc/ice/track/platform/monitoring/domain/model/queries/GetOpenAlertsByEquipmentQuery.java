package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

/**
 * Query to list the currently OPEN or ACKNOWLEDGED alerts for a given equipment.
 *
 * @param equipmentId identifier of the equipment
 */
public record GetOpenAlertsByEquipmentQuery(Long equipmentId) {
}
