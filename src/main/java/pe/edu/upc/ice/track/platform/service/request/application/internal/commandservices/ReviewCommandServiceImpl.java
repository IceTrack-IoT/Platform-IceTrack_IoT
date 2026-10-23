package pe.edu.upc.ice.track.platform.service.request.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.service.request.application.commandservices.ReviewCommandService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalUserProfileService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.SubmitReviewCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.UpdateReviewCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ReviewId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ReviewRating;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ReviewRepository;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ServiceRequestRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Review Command Service Implementation.
 *
 * <p>Both commands are owner-only. A review can only be submitted for a request the caller created,
 * and only edited by the owner who wrote it; anything else is reported as not found. The domain's
 * {@link IllegalStateException} - the request is not completed, or the edit window has closed - is
 * translated into {@code 409 REVIEW_NOT_ALLOWED}, its {@link IllegalArgumentException} into
 * {@code 400 VALIDATION_ERROR}. A second review of the same request is refused with
 * {@code 409 DUPLICATE_RESOURCE_CONFLICT}, and the unique constraint on {@code service_request_id}
 * settles the race the check cannot see, through {@code GlobalExceptionHandler}.</p>
 */
@Service
@Slf4j
public class ReviewCommandServiceImpl implements ReviewCommandService {

  private static final String REVIEW_RESOURCE = "Review";
  private static final String SERVICE_REQUEST_RESOURCE = "Service_Request";
  private static final String REVIEW_NOT_ALLOWED = "REVIEW_NOT_ALLOWED";

  private final ReviewRepository reviewRepository;
  private final ServiceRequestRepository serviceRequestRepository;
  private final ExternalUserProfileService externalUserProfileService;

  /**
   * Constructor
   *
   * @param reviewRepository           The {@link ReviewRepository} instance
   * @param serviceRequestRepository   The {@link ServiceRequestRepository} instance
   * @param externalUserProfileService The {@link ExternalUserProfileService} instance
   */
  public ReviewCommandServiceImpl(
      ReviewRepository reviewRepository,
      ServiceRequestRepository serviceRequestRepository,
      ExternalUserProfileService externalUserProfileService) {
    this.reviewRepository = reviewRepository;
    this.serviceRequestRepository = serviceRequestRepository;
    this.externalUserProfileService = externalUserProfileService;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Review, ApplicationError> handle(SubmitReviewCommand command) {
    return resolveOwnerId(command.username()).flatMap(ownerId -> {
      var owner = new OwnerId(ownerId);
      var request = findServiceRequest(command.serviceRequestId()).filter(candidate -> candidate.isOwnedBy(owner));
      if (request.isEmpty()) {
        return Result.failure(ApplicationError.notFound(
            SERVICE_REQUEST_RESOURCE, String.valueOf(command.serviceRequestId())));
      }
      var reviewed = request.get();
      if (reviewRepository.existsByServiceRequestId(reviewed.getId())) {
        return Result.failure(ApplicationError.duplicateResource(
            "Service request %s has already been reviewed; edit its review instead"
                .formatted(command.serviceRequestId())));
      }
      return attempt(() -> Review.submit(
              reviewed,
              owner,
              new ReviewRating(command.communication(), command.efficacy(), command.performance()),
              command.comment(),
              Instant.now()))
          .map(review -> persist("Submitted", review));
    });
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Review, ApplicationError> handle(UpdateReviewCommand command) {
    return resolveOwnerId(command.username()).flatMap(ownerId -> {
      var owner = new OwnerId(ownerId);
      var review = findReview(command.reviewId()).filter(candidate -> candidate.isOwnedBy(owner));
      if (review.isEmpty()) {
        return Result.failure(ApplicationError.notFound(REVIEW_RESOURCE, String.valueOf(command.reviewId())));
      }
      var edited = review.get();
      return attempt(() -> {
        edited.update(
            owner,
            new ReviewRating(command.communication(), command.efficacy(), command.performance()),
            command.comment(),
            Instant.now());
        return edited;
      }).map(updated -> persist("Updated", updated));
    });
  }

  /**
   * Runs a domain operation, translating the JDK exceptions the domain reports its failures with.
   *
   * @param operation the domain operation
   * @param <T>       the type of its outcome
   * @return the outcome; a 409 failure for an {@link IllegalStateException}, or a 400 failure for an
   *         {@link IllegalArgumentException}
   */
  private <T> Result<T, ApplicationError> attempt(Supplier<T> operation) {
    try {
      return Result.success(operation.get());
    } catch (IllegalStateException e) {
      return Result.failure(reviewNotAllowed(e.getMessage()));
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(REVIEW_RESOURCE, e.getMessage()));
    }
  }

  /**
   * Saves a review and logs what happened to it.
   *
   * @param action what was done, phrased as a past-tense verb
   * @param review the review to save
   * @return the saved review
   */
  private Review persist(String action, Review review) {
    var saved = reviewRepository.save(review);
    log.info("{} review {} of service request {}",
        action, saved.getId().reviewId(), saved.getServiceRequestId().serviceRequestId());
    return saved;
  }

  /**
   * Resolves the caller's username to the owner it acts as.
   *
   * @param username username of the requesting caller
   * @return the owner identifier, or a 403 failure when the caller holds no owner profile
   */
  private Result<Long, ApplicationError> resolveOwnerId(String username) {
    return externalUserProfileService.fetchOwnerIdByUsername(username)
        .<Result<Long, ApplicationError>>map(Result::success)
        .orElseGet(() -> Result.failure(noOwnerProfile()));
  }

  private Optional<ServiceRequest> findServiceRequest(Long serviceRequestId) {
    if (serviceRequestId == null || serviceRequestId <= 0) {
      return Optional.empty();
    }
    return serviceRequestRepository.findById(new ServiceRequestId(serviceRequestId));
  }

  private Optional<Review> findReview(Long reviewId) {
    if (reviewId == null || reviewId <= 0) {
      return Optional.empty();
    }
    return reviewRepository.findById(new ReviewId(reviewId));
  }

  /**
   * Builds the failure reported when a review cannot be written now.
   *
   * @param reason the explanation reported by the domain
   * @return a 409 failure
   */
  private static ApplicationError reviewNotAllowed(String reason) {
    return new ApplicationError(REVIEW_NOT_ALLOWED, "Review not allowed", reason);
  }

  /**
   * Builds the failure reported when the caller's account holds no owner profile.
   *
   * @return a 403 failure
   */
  private static ApplicationError noOwnerProfile() {
    return ApplicationError.accessDenied(
        "The authenticated account has no owner profile, so it owns no service request");
  }
}
