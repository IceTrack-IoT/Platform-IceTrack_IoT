package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ReviewResource;

/**
 * Assembler to convert a {@link Review} aggregate to a {@link ReviewResource}.
 */
public class ReviewResourceFromEntityAssembler {

  /**
   * Converts a {@link Review} aggregate to a {@link ReviewResource}.
   *
   * <p>The average is rounded to two decimals for display; the scores themselves are exposed as
   * they were given.</p>
   *
   * @param entity the {@link Review} aggregate to convert
   * @return the {@link ReviewResource} resource
   */
  public static ReviewResource toResourceFromEntity(Review entity) {
    var rating = entity.getRating();
    return new ReviewResource(
        entity.getId().reviewId(),
        entity.getServiceRequestId().serviceRequestId(),
        entity.getOwnerId().ownerId(),
        entity.getTechnicianId().technicianProfileId(),
        rating.communication(),
        rating.efficacy(),
        rating.performance(),
        Math.round(rating.average() * 100.0) / 100.0,
        entity.getComment(),
        entity.getCreatedAt().toString(),
        entity.getEditableUntil().toString());
  }
}
