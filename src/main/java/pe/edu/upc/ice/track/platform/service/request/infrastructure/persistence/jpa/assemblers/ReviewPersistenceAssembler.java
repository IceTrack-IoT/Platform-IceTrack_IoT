package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ReviewId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ReviewRating;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.ReviewPersistenceEntity;

/**
 * Static assembler between the {@link Review} aggregate and its {@link ReviewPersistenceEntity}.
 *
 * <p>The three scores are split into their own columns on the way out and joined back into a
 * single {@link ReviewRating} on the way in, so the 1-to-5 rule is re-established by the value
 * object on every read.</p>
 */
public final class ReviewPersistenceAssembler {

  private ReviewPersistenceAssembler() {
  }

  /**
   * Rebuilds the aggregate from a persisted row.
   *
   * @param entity the persisted row; may be {@code null}
   * @return the aggregate, or {@code null} when there is no row
   */
  public static Review toDomainFromPersistence(ReviewPersistenceEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Review(
        new ReviewId(entity.getId()),
        new ServiceRequestId(entity.getServiceRequestId()),
        new OwnerId(entity.getOwnerId()),
        new TechnicianProfileId(entity.getTechnicianId()),
        new ReviewRating(entity.getCommunication(), entity.getEfficacy(), entity.getPerformance()),
        entity.getComment(),
        entity.getSubmittedAt(),
        entity.getEditableUntil());
  }

  /**
   * Copies the state of a review onto its persistence entity, which is either new or the managed
   * entity the review was loaded from.
   *
   * @param review the review
   * @param entity the persistence entity to update
   */
  public static void copyToPersistence(Review review, ReviewPersistenceEntity entity) {
    entity.setServiceRequestId(review.getServiceRequestId().serviceRequestId());
    entity.setOwnerId(review.getOwnerId().ownerId());
    entity.setTechnicianId(review.getTechnicianId().technicianProfileId());
    entity.setCommunication(review.getRating().communication());
    entity.setEfficacy(review.getRating().efficacy());
    entity.setPerformance(review.getRating().performance());
    entity.setComment(review.getComment());
    entity.setSubmittedAt(review.getCreatedAt());
    entity.setEditableUntil(review.getEditableUntil());
  }
}
