package pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Outbound service (of the Anti-Corruption Layer between {@code reporting} and the
 * <strong>Service Request and Feedback Management</strong> bounded context) through which
 * {@code MaintenanceComplianceStrategy} and {@code TechnicianPerformanceStrategy} read
 * preventive-maintenance completions and technician throughput/ratings.
 *
 * <p>This is the only class in {@code reporting} that knows a {@code service.request} context
 * exists. It talks to it exclusively through {@link ServiceRequestContextFacade}: no service
 * request aggregate, repository or command is reachable from here.</p>
 *
 * <p><strong>Time translation.</strong> Reporting expresses its window as {@link LocalDateTime}
 * - the same local wall-clock time Monitoring stamps its alerts with through
 * {@code LocalDateTime.now()} - while Service Request and Feedback Management stores absolute
 * {@link Instant}s. This class is the single place where one is turned into the other, reading
 * the local time in the JVM default zone, which is the zone those {@code LocalDateTime.now()}
 * stamps were taken in.</p>
 */
@Service
public class ExternalServiceRequestsServiceForReporting {

  private final ServiceRequestContextFacade serviceRequestContextFacade;

  public ExternalServiceRequestsServiceForReporting(ServiceRequestContextFacade serviceRequestContextFacade) {
    this.serviceRequestContextFacade = serviceRequestContextFacade;
  }

  /**
   * @param siteId identifier of the site; required
   * @param from   inclusive start of the window
   * @param to     inclusive end of the window
   * @return identifiers of the site's units that have at least one completed preventive
   *         maintenance service request within the window
   */
  public List<Long> fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(
      Long siteId, LocalDateTime from, LocalDateTime to) {
    return serviceRequestContextFacade.fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(
        siteId, toInstant(from), toInstant(to));
  }

  /**
   * @param technicianId identifier of the technician; required
   * @param from         inclusive start of the window
   * @param to           inclusive end of the window
   * @return number of service requests this technician completed within the window
   */
  public long countCompletedServiceRequestsByTechnicianInRange(
      Long technicianId, LocalDateTime from, LocalDateTime to) {
    return serviceRequestContextFacade.countCompletedServiceRequestsByTechnicianInRange(
        technicianId, toInstant(from), toInstant(to));
  }

  /**
   * @param technicianId identifier of the technician; required
   * @return the technician's average review rating across their whole history, or empty when the
   *         technician has never been reviewed - an unrated technician is not a zero-rated one
   */
  public Optional<Double> fetchAverageRatingByTechnician(Long technicianId) {
    return Optional.ofNullable(serviceRequestContextFacade.fetchAverageRatingByTechnician(technicianId));
  }

  private static Instant toInstant(LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
  }
}
