package pe.edu.upc.ice.track.platform.service.request.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalUserProfileService;
import pe.edu.upc.ice.track.platform.service.request.application.queryservices.ReviewQueryService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.GetReviewByServiceRequestIdQuery;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.CallerRole;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ReviewRepository;

import java.util.Optional;

/**
 * Application service that resolves review read queries.
 *
 * <p>A review is read by the owner who wrote it and by the technician it is about; anybody else is
 * told there is no review, exactly as if the request had never been reviewed.</p>
 */
@Service
public class ReviewQueryServiceImpl implements ReviewQueryService {

  private final ReviewRepository reviewRepository;
  private final ExternalUserProfileService externalUserProfileService;

  /**
   * Creates the query service with its dependencies.
   *
   * @param reviewRepository           review repository port
   * @param externalUserProfileService outbound ACL service, used to resolve the caller to its profile
   */
  public ReviewQueryServiceImpl(
      ReviewRepository reviewRepository, ExternalUserProfileService externalUserProfileService) {
    this.reviewRepository = reviewRepository;
    this.externalUserProfileService = externalUserProfileService;
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public Optional<Review> handle(GetReviewByServiceRequestIdQuery query) {
    if (query.serviceRequestId() == null || query.serviceRequestId() <= 0) {
      return Optional.empty();
    }
    var serviceRequestId = new ServiceRequestId(query.serviceRequestId());
    if (query.callerRole() == CallerRole.TECHNICIAN) {
      return externalUserProfileService.fetchTechnicianIdByUsername(query.username())
          .map(TechnicianProfileId::new)
          .flatMap(technicianId -> reviewRepository.findByServiceRequestId(serviceRequestId)
              .filter(review -> review.concerns(technicianId)));
    }
    return externalUserProfileService.fetchOwnerIdByUsername(query.username())
        .map(OwnerId::new)
        .flatMap(ownerId -> reviewRepository.findByServiceRequestId(serviceRequestId)
            .filter(review -> review.isOwnedBy(ownerId)));
  }
}
