package pe.edu.upc.ice.track.platform.service.request.interfaces.acl;

import java.time.Instant;
import java.util.List;

/**
 * Published view of a service request with everything a service document reports: the request, its
 * status history, the intervention the technician registered and the owner's review. Returned by
 * {@link ServiceRequestContextFacade#fetchServiceInterventionRecord(Long)}.
 *
 * <p>Part of the published language of the {@code service.request} context: plain values only.
 * Statuses, types and priorities travel as the names of their constants.</p>
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
 * @param intervention        the registered intervention, or {@code null}
 * @param review              the owner's review, or {@code null}
 */
public record ServiceInterventionRecord(
    Long serviceRequestId,
    Long ownerProfileId,
    Long equipmentId,
    Long siteId,
    Long technicianProfileId,
    String type,
    String priority,
    String status,
    String description,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt,
    List<StatusChange> history,
    InterventionDetails intervention,
    ReviewDetails review) {

  /**
   * Keeps the history unmodifiable.
   */
  public ServiceInterventionRecord {
    history = history == null ? List.of() : List.copyOf(history);
  }

  /**
   * One status change of the request.
   *
   * @param fromStatus the status before, or {@code null} for the creation
   * @param toStatus   the status after
   * @param occurredAt when it happened
   * @param reason     why it happened, or {@code null}
   */
  public record StatusChange(String fromStatus, String toStatus, Instant occurredAt, String reason) {
  }

  /**
   * The technical report of the work.
   *
   * @param diagnosis     what the technician found
   * @param actionsTaken  what the technician did
   * @param spareParts    the spare parts used
   * @param photoEvidence references to the photos taken
   * @param startedAt     when the work started
   * @param completedAt   when the work finished
   */
  public record InterventionDetails(String diagnosis, String actionsTaken, List<String> spareParts,
                                    List<String> photoEvidence, Instant startedAt, Instant completedAt) {

    /**
     * Keeps the lists unmodifiable.
     */
    public InterventionDetails {
      spareParts = spareParts == null ? List.of() : List.copyOf(spareParts);
      photoEvidence = photoEvidence == null ? List.of() : List.copyOf(photoEvidence);
    }
  }

  /**
   * The owner's review of the technician.
   *
   * @param communication score from 1 to 5
   * @param efficacy      score from 1 to 5
   * @param performance   score from 1 to 5
   * @param average       mean of the three scores
   * @param comment       the free-text comment, or {@code null}
   * @param createdAt     when the review was submitted
   */
  public record ReviewDetails(int communication, int efficacy, int performance, double average,
                              String comment, Instant createdAt) {
  }
}
