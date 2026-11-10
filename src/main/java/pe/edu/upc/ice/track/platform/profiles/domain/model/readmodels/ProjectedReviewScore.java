package pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels;

import java.time.Instant;

/**
 * The score of one review as last counted in a technician's metrics.
 *
 * <p>Kept per review so an edit replaces the review's previous score instead of adding a second one,
 * and so an older event delivered after a newer one is recognized and ignored.</p>
 *
 * @param reviewId            the review
 * @param technicianProfileId the technician it is about
 * @param score               the review's average score
 * @param scoredAt            when the review was submitted or last edited, for that score
 */
public record ProjectedReviewScore(Long reviewId, Long technicianProfileId, double score, Instant scoredAt) {
}
