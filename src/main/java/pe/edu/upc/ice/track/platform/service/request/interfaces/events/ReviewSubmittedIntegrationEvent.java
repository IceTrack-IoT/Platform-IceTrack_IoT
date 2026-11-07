package pe.edu.upc.ice.track.platform.service.request.interfaces.events;

import java.time.Instant;

/**
 * Integration event published when an owner submits the review of a completed service request.
 *
 * <p>Delivered through the transactional outbox, at least once: Profiles adds the score to the
 * technician's metrics read model, keyed by {@code reviewId}.</p>
 *
 * @param reviewId            the new review
 * @param serviceRequestId    the reviewed request
 * @param technicianProfileId the technician the review is about
 * @param averageRating       the mean of the review's three scores, between 1 and 5
 * @param submittedAt         when the review was submitted
 */
public record ReviewSubmittedIntegrationEvent(
    Long reviewId,
    Long serviceRequestId,
    Long technicianProfileId,
    Double averageRating,
    Instant submittedAt) {
}
