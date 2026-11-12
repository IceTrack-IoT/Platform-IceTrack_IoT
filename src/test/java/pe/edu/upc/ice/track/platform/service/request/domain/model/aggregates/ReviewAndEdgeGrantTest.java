package pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.EdgeGrant;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.EdgeGrantIssuedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.EdgeGrantRevokedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ReviewSubmittedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ReviewUpdatedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.*;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

class ReviewAndEdgeGrantTest {

  @Nested
  class ReviewTests {

    @Test
    @DisplayName("Review.submit(completed request, owner, rating, comment, now) -> unpersisted review with a 48 h edit window (AAA)")
    void submit() {
      // Arrange
      var submittedAt = Instant.now();

      // Act
      var review = Review.submit(completed(), owner(), rating(), "  Great  ", submittedAt);

      // Assert
      assertThat(review.getId()).isNull();
      assertThat(review.getServiceRequestId()).isEqualTo(new ServiceRequestId(REQUEST_ID));
      assertThat(review.getTechnicianId()).isEqualTo(technician());
      assertThat(review.getComment()).isEqualTo("Great");
      assertThat(review.getCreatedAt()).isEqualTo(submittedAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
      assertThat(review.getEditableUntil()).isEqualTo(review.getCreatedAt().plus(Duration.ofHours(48)));
      assertThat(review.domainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Review.submit(...) guards -> not the owner, not COMPLETED, null request / now (AAA)")
    void submitGuards() {
      // Act & Assert
      assertThatThrownBy(() -> Review.submit(completed(), new OwnerId(OTHER_OWNER_ID), rating(), null, now()))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("Only the owner who created the service request may review it");
      assertThatThrownBy(() -> Review.submit(inProgress(), owner(), rating(), null, now()))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Only a completed service request can be reviewed; its status is IN_PROGRESS");
      assertThatThrownBy(() -> Review.submit(null, owner(), rating(), null, now())).hasMessage("Review serviceRequest must not be null");
      assertThatThrownBy(() -> Review.submit(completed(), owner(), rating(), null, null)).hasMessage("Review now must not be null");
      assertThatThrownBy(() -> Review.submit(completed(), owner(), null, null, now())).hasMessage("Review rating must not be null");
    }

    @Test
    @DisplayName("new Review(...) comment rules -> blank is null, 1000 accepted, 1001 rejected (AAA)")
    void comment() {
      // Act & Assert
      assertThat(Review.submit(completed(), owner(), rating(), "   ", now()).getComment()).isNull();
      assertThat(Review.submit(completed(), owner(), rating(), "x".repeat(1000), now()).getComment()).hasSize(1000);
      assertThatThrownBy(() -> Review.submit(completed(), owner(), rating(), "x".repeat(1001), now()))
          .hasMessage("Review comment must not exceed 1000 characters");
    }

    @Test
    @DisplayName("new Review(editableUntil not after createdAt) -> IllegalArgumentException (AAA)")
    void windowMustCloseAfterCreation() {
      // Arrange
      var at = now();

      // Act & Assert
      assertThatThrownBy(() -> new Review(null, new ServiceRequestId(1L), owner(), technician(), rating(), null, at, at))
          .hasMessage("Review edit window must close after the review was submitted");
    }

    @Test
    @DisplayName("onSubmitted() on a persisted review -> registers ReviewSubmittedEvent with the average (AAA)")
    void onSubmitted() {
      // Arrange
      var review = review();

      // Act
      review.onSubmitted();

      // Assert
      var event = (ReviewSubmittedEvent) review.domainEvents().iterator().next();
      assertThat(event.reviewId()).isEqualTo(REVIEW_ID);
      assertThat(event.serviceRequestId()).isEqualTo(REQUEST_ID);
      assertThat(event.ownerId()).isEqualTo(OWNER_ID);
      assertThat(event.technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(event.averageRating()).isEqualTo(4.0);
      assertThat(event.submittedAt()).isEqualTo(review.getCreatedAt());
    }

    @Test
    @DisplayName("onSubmitted() before persistence -> IllegalStateException (AAA)")
    void onSubmittedWithoutId() {
      // Act & Assert
      assertThatThrownBy(() -> Review.submit(completed(), owner(), rating(), null, now()).onSubmitted())
          .isInstanceOf(IllegalStateException.class).hasMessage("A review must be persisted before its submission is announced");
    }

    @Test
    @DisplayName("update(owner, rating, comment, now) inside the window -> new rating and comment, ReviewUpdatedEvent (AAA)")
    void update() {
      // Arrange
      var review = review();
      var at = review.getCreatedAt().plus(Duration.ofHours(47));

      // Act
      review.update(owner(), new ReviewRating(1, 1, 1), "  Changed my mind ", at);

      // Assert
      assertThat(review.getRating()).isEqualTo(new ReviewRating(1, 1, 1));
      assertThat(review.getComment()).isEqualTo("Changed my mind");
      var event = (ReviewUpdatedEvent) review.domainEvents().iterator().next();
      assertThat(event.averageRating()).isEqualTo(1.0);
      assertThat(event.updatedAt()).isEqualTo(at);
      assertThat(event.reviewId()).isEqualTo(REVIEW_ID);
    }

    @Test
    @DisplayName("update(...) guards -> not the owner, window closed (boundary included), null rating (AAA)")
    void updateGuards() {
      // Arrange
      var review = review();
      var closed = review.getEditableUntil();

      // Act & Assert
      assertThatThrownBy(() -> review.update(new OwnerId(OTHER_OWNER_ID), rating(), null, now()))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("Only the owner who wrote the review may edit it");
      assertThatThrownBy(() -> review.update(owner(), rating(), null, closed))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("The review could only be edited until %s".formatted(closed));
      assertThatThrownBy(() -> review.update(owner(), null, null, now())).hasMessage("Review rating must not be null");
      assertThatThrownBy(() -> review.update(owner(), rating(), null, null)).hasMessage("Review now must not be null");
      assertThat(review.getRating()).isEqualTo(rating());
      assertThat(review.domainEvents()).isEmpty();
    }

    @Test
    @DisplayName("isEditableAt / isOwnedBy / concerns -> boundary and identity checks (AAA)")
    void predicates() {
      // Arrange
      var review = review();

      // Act & Assert
      assertThat(review.isEditableAt(review.getEditableUntil().minusNanos(1000))).isTrue();
      assertThat(review.isEditableAt(review.getEditableUntil())).isFalse();
      assertThat(review.isEditableAt(null)).isFalse();
      assertThat(review.isOwnedBy(owner())).isTrue();
      assertThat(review.isOwnedBy(new OwnerId(OTHER_OWNER_ID))).isFalse();
      assertThat(review.concerns(technician())).isTrue();
      assertThat(review.concerns(new TechnicianProfileId(OTHER_TECHNICIAN_ID))).isFalse();
      assertThat(review.getId()).isEqualTo(new ReviewId(REVIEW_ID));
    }
  }

  @Nested
  class EdgeGrantTests {

    @Test
    @DisplayName("EdgeGrant.issue(...) -> unrevoked grant with micro-truncated times and EdgeGrantIssuedEvent (AAA)")
    void issue() {
      // Arrange
      var issuedAt = Instant.now();

      // Act
      var grant = EdgeGrant.issue(REQUEST_ID, TECHNICIAN_ID, EQUIPMENT_ID, DIGEST, issuedAt, issuedAt.plusSeconds(3600));

      // Assert
      assertThat(grant.getId()).isNull();
      assertThat(grant.isRevoked()).isFalse();
      assertThat(grant.getIssuedAt()).isEqualTo(issuedAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
      assertThat(grant.domainEvents()).containsExactly(new EdgeGrantIssuedEvent(REQUEST_ID, TECHNICIAN_ID, EQUIPMENT_ID));
      assertThat(EdgeGrant.TOKEN_PREFIX).isEqualTo("it_grt_");
    }

    @Test
    @DisplayName("new EdgeGrant(invalid digest / times / null parts) -> IllegalArgumentException (AAA)")
    void invalid() {
      // Arrange
      var at = now();

      // Act & Assert
      assertThatThrownBy(() -> new EdgeGrant(null, 1L, 1L, 1L, "xyz", at, at.plusSeconds(1), null, null))
          .hasMessage("Edge grant tokenDigest must be a hex SHA-256 digest");
      assertThatThrownBy(() -> new EdgeGrant(null, 1L, 1L, 1L, "A".repeat(64), at, at.plusSeconds(1), null, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new EdgeGrant(null, 1L, 1L, 1L, null, at, at.plusSeconds(1), null, null))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new EdgeGrant(null, 1L, 1L, 1L, DIGEST, at, at, null, null))
          .hasMessage("Edge grant expiresAt must be after issuedAt");
      assertThatThrownBy(() -> new EdgeGrant(null, null, 1L, 1L, DIGEST, at, at.plusSeconds(1), null, null))
          .hasMessage("Edge grant serviceRequestId must not be null");
      assertThatThrownBy(() -> new EdgeGrant(null, 1L, null, 1L, DIGEST, at, at.plusSeconds(1), null, null))
          .hasMessage("Edge grant technicianProfileId must not be null");
      assertThatThrownBy(() -> new EdgeGrant(null, 1L, 1L, null, DIGEST, at, at.plusSeconds(1), null, null))
          .hasMessage("Edge grant equipmentId must not be null");
    }

    @Test
    @DisplayName("revoke(reason, now) -> revoked with reason and EdgeGrantRevokedEvent (AAA)")
    void revoke() {
      // Arrange
      var grant = grant();
      var at = Instant.now();

      // Act
      grant.revoke("CANCELED", at);

      // Assert
      assertThat(grant.isRevoked()).isTrue();
      assertThat(grant.getRevocationReason()).isEqualTo("CANCELED");
      assertThat(grant.getRevokedAt()).isEqualTo(at.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
      assertThat(grant.domainEvents()).containsExactly(new EdgeGrantRevokedEvent(REQUEST_ID, TECHNICIAN_ID, EQUIPMENT_ID, "CANCELED"));
    }

    @Test
    @DisplayName("revoke(...) again -> no-op keeping the first reason and a single event (AAA)")
    void revokeTwice() {
      // Arrange
      var grant = grant();
      grant.revoke("CANCELED", now());

      // Act
      grant.revoke("COMPLETED", now().plusSeconds(10));

      // Assert
      assertThat(grant.getRevocationReason()).isEqualTo("CANCELED");
      assertThat(grant.domainEvents()).hasSize(1);
    }

    @Test
    @DisplayName("revoke(blank / over 30 chars / null now) -> IllegalArgumentException (AAA)")
    void revokeGuards() {
      // Act & Assert
      assertThatThrownBy(() -> grant().revoke(" ", now())).hasMessage("An edge grant revocation reason is required");
      assertThatThrownBy(() -> grant().revoke(null, now())).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> grant().revoke("x".repeat(31), now()))
          .hasMessage("The edge grant revocation reason must not exceed 30 characters");
      grant().revoke("x".repeat(30), now());
      assertThatThrownBy(() -> grant().revoke("R", null)).hasMessage("Edge grant now must not be null");
    }

    @Test
    @DisplayName("isActiveAt(instant) -> active before expiry unless revoked (AAA)")
    void active() {
      // Arrange
      var grant = grant();

      // Act & Assert
      assertThat(grant.isActiveAt(grant.getIssuedAt())).isTrue();
      assertThat(grant.isActiveAt(grant.getExpiresAt().minusNanos(1000))).isTrue();
      assertThat(grant.isActiveAt(grant.getExpiresAt())).isFalse();
      grant.revoke("REASSIGNED", now());
      assertThat(grant.isActiveAt(grant.getIssuedAt())).isFalse();
    }
  }
}
