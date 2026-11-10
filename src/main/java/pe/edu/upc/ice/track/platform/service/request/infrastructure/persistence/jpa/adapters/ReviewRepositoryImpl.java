package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ReviewId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ReviewRepository;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.assemblers.ReviewPersistenceAssembler;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.ReviewPersistenceEntity;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.projections.ServiceRequestIdProjection;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.repositories.ReviewPersistenceRepository;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repository adapter that bridges the {@link ReviewRepository} port with Spring Data JPA.
 *
 * <p>Like {@code ServiceRequestRepositoryImpl}, an existing review is saved by updating its managed
 * entity, and this adapter is the event-publishing boundary: the {@code ReviewSubmittedEvent} of a
 * new review is raised on the rehydrated copy once the insert assigned its identity, and the
 * {@code ReviewUpdatedEvent} of an edit is dispatched right after the row is written.</p>
 */
@Repository
public class ReviewRepositoryImpl implements ReviewRepository {

  /**
   * Number of criteria a review scores - communication, efficacy and performance - which is what
   * {@code ReviewRating.average()} divides by.
   */
  private static final double RATING_CRITERIA_COUNT = 3.0;

  private final ReviewPersistenceRepository reviewPersistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructor
   *
   * @param reviewPersistenceRepository the Spring Data repository
   * @param eventPublisher              the Spring event publisher used to dispatch domain events
   */
  public ReviewRepositoryImpl(
      ReviewPersistenceRepository reviewPersistenceRepository, ApplicationEventPublisher eventPublisher) {
    this.reviewPersistenceRepository = reviewPersistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  // inherited javadoc
  @Override
  public Optional<Review> findById(ReviewId reviewId) {
    if (reviewId == null) {
      return Optional.empty();
    }
    return reviewPersistenceRepository.findById(reviewId.reviewId())
        .map(ReviewPersistenceAssembler::toDomainFromPersistence);
  }

  // inherited javadoc
  @Override
  public Optional<Review> findByServiceRequestId(ServiceRequestId serviceRequestId) {
    if (serviceRequestId == null) {
      return Optional.empty();
    }
    return reviewPersistenceRepository.findByServiceRequestId(serviceRequestId.serviceRequestId())
        .map(ReviewPersistenceAssembler::toDomainFromPersistence);
  }

  // inherited javadoc
  @Override
  public boolean existsByServiceRequestId(ServiceRequestId serviceRequestId) {
    return serviceRequestId != null
        && reviewPersistenceRepository.existsByServiceRequestId(serviceRequestId.serviceRequestId());
  }

  // inherited javadoc
  @Override
  public Set<Long> findReviewedServiceRequestIds(Collection<Long> serviceRequestIds) {
    if (serviceRequestIds == null || serviceRequestIds.isEmpty()) {
      return Set.of();
    }
    var distinct = serviceRequestIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    if (distinct.isEmpty()) {
      return Set.of();
    }
    return reviewPersistenceRepository.findByServiceRequestIdIn(distinct).stream()
        .map(ServiceRequestIdProjection::getServiceRequestId)
        .collect(Collectors.toUnmodifiableSet());
  }

  // inherited javadoc
  @Override
  public Optional<Double> findAverageRatingByTechnicianId(TechnicianProfileId technicianId) {
    if (technicianId == null) {
      return Optional.empty();
    }
    var reviews = reviewPersistenceRepository.findScoresByTechnicianId(technicianId.technicianProfileId());
    if (reviews.isEmpty()) {
      return Optional.empty();
    }
    // The mean of the per-review means equals the sum of every score over (reviews x criteria),
    // which keeps the accumulation exact in integers until the single final division.
    long scoreTotal = reviews.stream()
        .mapToLong(review -> (long) review.getCommunication() + review.getEfficacy() + review.getPerformance())
        .sum();
    return Optional.of(scoreTotal / (reviews.size() * RATING_CRITERIA_COUNT));
  }

  /**
   * Persists a review and dispatches whatever domain events the change raised.
   *
   * @param review the review to persist
   * @return the persisted review
   * @throws OptimisticLockingFailureException when the review disappeared after it was read
   */
  @Override
  @Transactional
  public Review save(Review review) {
    var isNew = review.getId() == null;
    var entity = isNew
        ? new ReviewPersistenceEntity()
        : reviewPersistenceRepository.findById(review.getId().reviewId())
            .orElseThrow(() -> new OptimisticLockingFailureException(
                "Review %s was deleted after it was read".formatted(review.getId().reviewId())));
    ReviewPersistenceAssembler.copyToPersistence(review, entity);
    // Flushed so that a second review of the same request is rejected by the unique constraint
    // here, inside the caller's transaction.
    var savedEntity = reviewPersistenceRepository.saveAndFlush(entity);
    var savedReview = ReviewPersistenceAssembler.toDomainFromPersistence(savedEntity);
    publishPendingEvents(review);
    if (isNew) {
      savedReview.onSubmitted();
      publishPendingEvents(savedReview);
    }
    return savedReview;
  }

  /**
   * Dispatches the events the aggregate registered, then clears them so a later save of the same
   * aggregate does not announce the same change twice.
   *
   * @param review the aggregate that was just persisted
   */
  private void publishPendingEvents(Review review) {
    review.domainEvents().forEach(eventPublisher::publishEvent);
    review.clearDomainEvents();
  }
}
