package pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceInterventionRecord;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound service (of the Anti-Corruption Layer between {@code reporting} and the
 * <strong>Service Request and Feedback Management</strong> bounded context) through which
 * {@code MaintenanceComplianceStrategy} and {@code TechnicianPerformanceStrategy} read
 * preventive-maintenance completions and technician throughput/ratings, and through which a service
 * document reads one request with its history, intervention and review.
 *
 * <p>This is the only class in {@code reporting} that knows a {@code service.request} context
 * exists. It talks to it exclusively through {@link ServiceRequestContextFacade}: no service
 * request aggregate, repository or command is reachable from here.</p>
 *
 * <p>Reporting and Service Request and Feedback Management both express moments as UTC
 * {@link Instant}s, so a report window is passed through unchanged.</p>
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
      Long siteId, Instant from, Instant to) {
    return serviceRequestContextFacade.fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(
        siteId, from, to);
  }

  /**
   * @param technicianId identifier of the technician; required
   * @param from         inclusive start of the window
   * @param to           inclusive end of the window
   * @return number of service requests this technician completed within the window
   */
  public long countCompletedServiceRequestsByTechnicianInRange(
      Long technicianId, Instant from, Instant to) {
    return serviceRequestContextFacade.countCompletedServiceRequestsByTechnicianInRange(
        technicianId, from, to);
  }

  /**
   * @param technicianId identifier of the technician; required
   * @return the technician's average review rating across their whole history, or empty when the
   *         technician has never been reviewed - an unrated technician is not a zero-rated one
   */
  public Optional<Double> fetchAverageRatingByTechnician(Long technicianId) {
    return Optional.ofNullable(serviceRequestContextFacade.fetchAverageRatingByTechnician(technicianId));
  }

  /**
   * Reads a service request with its history, intervention and review, translated into this
   * context's own terms.
   *
   * @param serviceRequestId identifier of the request
   * @return the request, or empty when it does not exist
   */
  public Optional<ServiceIntervention> fetchServiceIntervention(Long serviceRequestId) {
    return serviceRequestContextFacade.fetchServiceInterventionRecord(serviceRequestId)
        .map(ExternalServiceRequestsServiceForReporting::toServiceIntervention);
  }

  private static ServiceIntervention toServiceIntervention(ServiceInterventionRecord record) {
    var intervention = record.intervention();
    var review = record.review();
    return new ServiceIntervention(
        record.serviceRequestId(),
        record.ownerProfileId(),
        record.equipmentId(),
        record.siteId(),
        record.technicianProfileId(),
        record.type(),
        record.priority(),
        record.status(),
        record.description(),
        record.createdAt(),
        record.startedAt(),
        record.completedAt(),
        record.history().stream()
            .map(change -> new ServiceIntervention.StatusChange(
                change.fromStatus(), change.toStatus(), change.occurredAt(), change.reason()))
            .toList(),
        intervention == null ? null : new ServiceIntervention.Work(
            intervention.diagnosis(), intervention.actionsTaken(), intervention.spareParts(),
            intervention.photoEvidence(), intervention.startedAt(), intervention.completedAt()),
        review == null ? null : new ServiceIntervention.Rating(
            review.communication(), review.efficacy(), review.performance(), review.average(),
            review.comment(), review.createdAt()));
  }

  /**
   * A service request as the service document reports it, in this context's own terms.
   *
   * @param serviceRequestId    the request
   * @param ownerProfileId      the owner who created it
   * @param equipmentId         the unit it was raised for
   * @param siteId              the site of that unit
   * @param technicianProfileId the technician assigned to it, or {@code null}
   * @param type                the kind of work
   * @param priority            the urgency
   * @param status              the current status
   * @param description         what the owner asked for
   * @param createdAt           when the request was created
   * @param startedAt           when the work started, or {@code null}
   * @param completedAt         when the work finished, or {@code null}
   * @param history             the status changes, oldest first
   * @param work                the intervention, or {@code null}
   * @param rating              the owner's review, or {@code null}
   */
  public record ServiceIntervention(
      Long serviceRequestId, Long ownerProfileId, Long equipmentId, Long siteId, Long technicianProfileId,
      String type, String priority, String status, String description, Instant createdAt, Instant startedAt,
      Instant completedAt, List<StatusChange> history, Work work, Rating rating) {

    /** The status every service document requires. */
    public static final String COMPLETED = "COMPLETED";

    /**
     * Tells whether the request is completed.
     *
     * @return {@code true} when its status is {@code COMPLETED}
     */
    public boolean isCompleted() {
      return COMPLETED.equals(status);
    }

    /**
     * The request's own period: from when the work started (or the request was created) to when it
     * was completed.
     *
     * @return the start of the period, never {@code null}
     */
    public Instant periodStart() {
      if (work != null && work.startedAt() != null) {
        return work.startedAt();
      }
      return startedAt != null ? startedAt : createdAt;
    }

    /**
     * @return the end of the period; the completion instant, or the start of the period when unknown
     */
    public Instant periodEnd() {
      if (completedAt != null) {
        return completedAt;
      }
      return work != null && work.completedAt() != null ? work.completedAt() : periodStart();
    }

    /** One status change. */
    public record StatusChange(String fromStatus, String toStatus, Instant occurredAt, String reason) {
    }

    /** The intervention the technician registered. */
    public record Work(String diagnosis, String actionsTaken, List<String> spareParts, List<String> photoEvidence,
                       Instant startedAt, Instant completedAt) {
    }

    /** The owner's review. */
    public record Rating(int communication, int efficacy, int performance, double average, String comment,
                         Instant createdAt) {
    }
  }
}
