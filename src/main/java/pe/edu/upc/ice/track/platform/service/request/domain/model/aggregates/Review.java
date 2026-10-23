package pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ReviewSubmittedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ReviewUpdatedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ReviewId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ReviewRating;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Review aggregate root of the {@code service.request} bounded context.
 *
 * <p>The owner's assessment of the technician who completed one of its service requests. A request
 * receives at most one review - the {@code service_request_reviews} table carries a unique
 * constraint on {@code service_request_id} - and the review may be edited for
 * {@link #EDIT_WINDOW} after it was submitted, never afterward.</p>
 *
 * <p>A separate aggregate from {@link ServiceRequest}, referenced by identifier: the request is
 * closed once completed, while its review keeps changing for two more days, and the two must not
 * contend for the same optimistic lock.</p>
 *
 * <p><strong>Failures.</strong> {@link IllegalArgumentException} when a value is invalid or the
 * actor is not the owner, {@link IllegalStateException} when the review is not allowed - the request
 * is not completed, or the edit window has closed.</p>
 *
 * <p>No JPA or persistence annotation is present here - the mapping lives exclusively in
 * {@code ReviewPersistenceEntity}.</p>
 */
@Getter
public class Review extends AbstractDomainAggregateRoot<Review> {

  /** How long after its submission a review may still be edited. */
  public static final Duration EDIT_WINDOW = Duration.ofHours(48);

  /** Maximum number of characters the comment may hold, matching the column width. */
  public static final int MAX_COMMENT_LENGTH = 1000;

  private final ReviewId id;
  private final ServiceRequestId serviceRequestId;
  private final OwnerId ownerId;
  private final TechnicianProfileId technicianId;
  private ReviewRating rating;
  private String comment;
  private final Instant createdAt;
  private final Instant editableUntil;

  /**
   * Reconstitutes a review, typically from persistence.
   *
   * <p>New reviews must be created through {@link #submit}.</p>
   *
   * @param id               the persistence identity, or {@code null} for a review not yet persisted
   * @param serviceRequestId the reviewed request; required
   * @param ownerId          the owner who wrote the review; required
   * @param technicianId     the technician the review is about; required
   * @param rating           the scores; required
   * @param comment          the free-text comment, may be {@code null}; a blank value is stored as
   *                         {@code null}
   * @param createdAt        when the review was submitted; required
   * @param editableUntil    the instant the edit window closes; required and after {@code createdAt}
   * @throws IllegalArgumentException when a required value is missing or invalid
   */
  public Review(ReviewId id, ServiceRequestId serviceRequestId, OwnerId ownerId, TechnicianProfileId technicianId,
                ReviewRating rating, String comment, Instant createdAt, Instant editableUntil) {
    this.id = id;
    this.serviceRequestId = requireValue(serviceRequestId, "serviceRequestId");
    this.ownerId = requireValue(ownerId, "ownerId");
    this.technicianId = requireValue(technicianId, "technicianId");
    this.rating = requireValue(rating, "rating");
    this.comment = normalizedComment(comment);
    this.createdAt = requireValue(createdAt, "createdAt");
    this.editableUntil = requireValue(editableUntil, "editableUntil");
    if (!editableUntil.isAfter(createdAt)) {
      throw new IllegalArgumentException("Review edit window must close after the review was submitted");
    }
  }

  /**
   * Submits the review of a completed service request.
   *
   * <p>The technician reviewed is the one who completed the request. The edit window opens now and
   * closes {@link #EDIT_WINDOW} later.</p>
   *
   * @param request the reviewed request; required
   * @param ownerId the owner submitting the review; must be the request's owner
   * @param rating  the scores; required
   * @param comment the free-text comment, may be {@code null}
   * @param now     the current instant; required
   * @return the new review, not yet persisted
   * @throws IllegalArgumentException when a value is invalid or the owner did not create the request
   * @throws IllegalStateException    when the request is not {@code COMPLETED}
   */
  public static Review submit(ServiceRequest request, OwnerId ownerId, ReviewRating rating, String comment,
                              Instant now) {
    requireValue(request, "serviceRequest");
    requireValue(now, "now");
    if (!request.isOwnedBy(ownerId)) {
      throw new IllegalArgumentException("Only the owner who created the service request may review it");
    }
    if (request.getStatus() != ServiceStatus.COMPLETED) {
      throw new IllegalStateException(
          "Only a completed service request can be reviewed; its status is %s".formatted(request.getStatus()));
    }
    var submittedAt = now.truncatedTo(ChronoUnit.MICROS);
    return new Review(null, request.getId(), ownerId, request.getTechnicianId(), rating, comment,
        submittedAt, submittedAt.plus(EDIT_WINDOW));
  }

  /**
   * Replaces the scores and the comment of this review.
   *
   * @param actorOwnerId the owner editing; must be the one who wrote the review
   * @param rating       the new scores; required
   * @param comment      the new comment, may be {@code null}
   * @param now          the current instant; required and before {@code editableUntil}
   * @throws IllegalArgumentException when a value is invalid or the actor did not write the review
   * @throws IllegalStateException    when the edit window has closed
   */
  public void update(OwnerId actorOwnerId, ReviewRating rating, String comment, Instant now) {
    if (!isOwnedBy(actorOwnerId)) {
      throw new IllegalArgumentException("Only the owner who wrote the review may edit it");
    }
    requireValue(now, "now");
    if (!isEditableAt(now)) {
      throw new IllegalStateException("The review could only be edited until %s".formatted(editableUntil));
    }
    this.rating = requireValue(rating, "rating");
    this.comment = normalizedComment(comment);
    registerDomainEvent(ReviewUpdatedEvent.from(this, now));
  }

  /**
   * Tells whether this review may still be edited at the given instant.
   *
   * @param instant the instant to test
   * @return {@code true} when the instant lies strictly before {@code editableUntil}
   */
  public boolean isEditableAt(Instant instant) {
    return instant != null && instant.isBefore(editableUntil);
  }

  /**
   * Tells whether this review was written by the given owner.
   *
   * @param candidate the owner to test
   * @return {@code true} when the identifiers match
   */
  public boolean isOwnedBy(OwnerId candidate) {
    return ownerId.equals(candidate);
  }

  /**
   * Tells whether this review is about the given technician.
   *
   * @param candidate the technician to test
   * @return {@code true} when the identifiers match
   */
  public boolean concerns(TechnicianProfileId candidate) {
    return technicianId.equals(candidate);
  }

  /**
   * Signals that this review has just been submitted and persisted.
   *
   * <p>Called by the repository adapter once the persistence identity is available, since the
   * submission event carries it.</p>
   */
  public void onSubmitted() {
    if (id == null) {
      throw new IllegalStateException("A review must be persisted before its submission is announced");
    }
    registerDomainEvent(ReviewSubmittedEvent.from(this));
  }

  private static String normalizedComment(String comment) {
    if (comment == null || comment.isBlank()) {
      return null;
    }
    var trimmed = comment.strip();
    if (trimmed.length() > MAX_COMMENT_LENGTH) {
      throw new IllegalArgumentException(
          "Review comment must not exceed %d characters".formatted(MAX_COMMENT_LENGTH));
    }
    return trimmed;
  }

  private static <T> T requireValue(T value, String field) {
    if (value == null) {
      throw new IllegalArgumentException("Review %s must not be null".formatted(field));
    }
    return value;
  }
}
