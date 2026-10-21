package pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.servicerequests;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbound port (of the Anti-Corruption Layer between {@code reporting} and the
 * <strong>Service Request and Feedback Management</strong> bounded context) through which
 * {@code MaintenanceComplianceStrategy} and {@code TechnicianPerformanceStrategy} would read
 * preventive-maintenance completions and technician throughput/ratings.
 *
 * <p><strong>Pending dependency.</strong> Service Request and Feedback Management has not been
 * built in this codebase yet, so there is no real context to adapt to. The port is declared now,
 * with the shape those two strategies were specified against, so they can already depend on a
 * stable abstraction instead of being written around a dependency that does not exist.
 * {@code ExternalServiceRequestsServiceForReportingImpl}, in
 * {@code reporting.infrastructure.servicerequests.services}, is the single adapter that will need
 * a real implementation once that context exists; until then it throws
 * {@link UnsupportedOperationException} for every method.</p>
 */
public interface ExternalServiceRequestsServiceForReporting {

  /**
   * @param siteId identifier of the site; required
   * @param from   inclusive start of the window
   * @param to     inclusive end of the window
   * @return identifiers of the site's units that have at least one completed preventive
   *         maintenance service request within the window
   */
  List<Long> fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(Long siteId, LocalDateTime from, LocalDateTime to);

  /**
   * @param technicianId identifier of the technician; required
   * @param from         inclusive start of the window
   * @param to           inclusive end of the window
   * @return number of service requests this technician completed within the window
   */
  long countCompletedServiceRequestsByTechnicianInRange(Long technicianId, LocalDateTime from, LocalDateTime to);

  /**
   * @param technicianId identifier of the technician; required
   * @return the technician's average review rating across their whole history
   */
  double fetchAverageRatingByTechnician(Long technicianId);
}
