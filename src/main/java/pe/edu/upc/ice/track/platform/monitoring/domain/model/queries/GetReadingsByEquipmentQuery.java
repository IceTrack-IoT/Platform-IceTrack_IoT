package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

import java.time.LocalDateTime;

/**
 * Query to fetch the reading history of an equipment within a date range, used by
 * Reporting & Análisis to compute temperature-trend and excursion KPIs.
 *
 * @param equipmentId identifier of the equipment
 * @param from        inclusive start of the range
 * @param to          inclusive end of the range
 */
public record GetReadingsByEquipmentQuery(Long equipmentId, LocalDateTime from, LocalDateTime to) {
}
