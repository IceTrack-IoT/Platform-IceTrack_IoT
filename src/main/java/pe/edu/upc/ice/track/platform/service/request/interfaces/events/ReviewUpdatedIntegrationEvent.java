package pe.edu.upc.ice.track.platform.service.request.interfaces.events;

import java.time.Instant;

/**
 * Integration event published when an owner edits a review within its edit window.
 *
 * <p>Delivered through the transactional outbox, at least once: Profiles replaces the review's score
 * in the technician's metrics read model, keyed by {@code reviewId}.</p>
 *
 * @param reviewId            the edited review
 * @param serviceRequestId    the reviewed request
 * @param technicianProfileId the technician the review is about
 * @param averageRating       the mean of the review's three new scores, between 1 and 5
 * @param updatedAt           when the review was edited
 */
public record ReviewUpdatedIntegrationEvent(
    Long reviewId,
    Long serviceRequestId,
    Long technicianProfileId,
    Double averageRating,
    Instant updatedAt) {
}
