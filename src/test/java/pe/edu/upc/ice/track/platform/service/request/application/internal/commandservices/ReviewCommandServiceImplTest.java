package pe.edu.upc.ice.track.platform.service.request.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.service.request.application.internal.commandservices.ReviewCommandServiceImpl;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalUserProfileService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.SubmitReviewCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.UpdateReviewCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ReviewId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ReviewRepository;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ServiceRequestRepository;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;

@ExtendWith(MockitoExtension.class)
class ReviewCommandServiceImplTest {

  private static final ServiceRequestId REQUEST = new ServiceRequestId(REQUEST_ID);

  @Mock private ReviewRepository reviews;
  @Mock private ServiceRequestRepository requests;
  @Mock private ExternalUserProfileService profiles;
  @InjectMocks private ReviewCommandServiceImpl service;

  private void asOwner() {
    doReturn(Optional.of(OWNER_ID)).when(profiles).fetchOwnerIdByUsername(USERNAME);
  }

  private void savedWithId() {
    doAnswer(inv -> {
      Review r = inv.getArgument(0);
      return new Review(new ReviewId(REVIEW_ID), r.getServiceRequestId(), r.getOwnerId(), r.getTechnicianId(),
          r.getRating(), r.getComment(), r.getCreatedAt(), r.getEditableUntil());
    }).when(reviews).save(any(Review.class));
  }

  @Nested
  class Submit {

    private final SubmitReviewCommand command = new SubmitReviewCommand(REQUEST_ID, USERNAME, 5, 4, 3, "  Good job ");

    @Test
    @DisplayName("handle(SubmitReviewCommand) -> persists a review of the completed request with a 48 h edit window (AAA)")
    void submits() {
      // Arrange
      asOwner();
      doReturn(Optional.of(completed())).when(requests).findById(REQUEST);
      doReturn(false).when(reviews).existsByServiceRequestId(REQUEST);
      savedWithId();

      // Act
      var review = successValue(service.handle(command));

      // Assert
      assertThat(review.getId()).isEqualTo(new ReviewId(REVIEW_ID));
      assertThat(review.getRating().average()).isEqualTo(4.0);
      assertThat(review.getComment()).isEqualTo("Good job");
      assertThat(review.getTechnicianId()).isEqualTo(technician());
      assertThat(Duration.between(review.getCreatedAt(), review.getEditableUntil())).isEqualTo(Duration.ofHours(48));
      verify(reviews, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("handle(SubmitReviewCommand) without owner profile -> ACCESS_DENIED (AAA)")
    void noOwner() {
      // Arrange
      doReturn(Optional.empty()).when(profiles).fetchOwnerIdByUsername(USERNAME);

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("ACCESS_DENIED");
      verifyNoInteractions(requests, reviews);
    }

    @Test
    @DisplayName("handle(SubmitReviewCommand) for a missing, foreign or invalid request id -> SERVICE_REQUEST_NOT_FOUND (AAA)")
    void requestNotReachable() {
      // Arrange
      asOwner();
      doReturn(Optional.empty()).when(requests).findById(REQUEST);
      doReturn(Optional.of(completed())).when(requests).findById(new ServiceRequestId(78L));
      doReturn(Optional.of(OTHER_OWNER_ID)).when(profiles).fetchOwnerIdByUsername("other");

      // Act
      var missing = failureError(service.handle(command));
      var zero = failureError(service.handle(new SubmitReviewCommand(0L, USERNAME, 5, 5, 5, null)));
      var foreign = failureError(service.handle(new SubmitReviewCommand(78L, "other", 5, 5, 5, null)));

      // Assert
      assertThat(missing.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      assertThat(missing.message()).isEqualTo("Service_Request not found: 77");
      assertThat(zero.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      assertThat(foreign.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      verify(reviews, never()).save(any());
    }

    @Test
    @DisplayName("handle(SubmitReviewCommand) for an already reviewed request -> DUPLICATE_RESOURCE_CONFLICT (AAA)")
    void alreadyReviewed() {
      // Arrange
      asOwner();
      doReturn(Optional.of(completed())).when(requests).findById(REQUEST);
      doReturn(true).when(reviews).existsByServiceRequestId(REQUEST);

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("DUPLICATE_RESOURCE_CONFLICT");
      assertThat(error.details()).isEqualTo("Service request 77 has already been reviewed; edit its review instead");
      verify(reviews, never()).save(any());
    }

    @Test
    @DisplayName("handle(SubmitReviewCommand) for a request not COMPLETED -> REVIEW_NOT_ALLOWED (AAA)")
    void notCompleted() {
      // Arrange
      asOwner();
      doReturn(Optional.of(inProgress())).when(requests).findById(REQUEST);
      doReturn(false).when(reviews).existsByServiceRequestId(REQUEST);

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("REVIEW_NOT_ALLOWED");
      assertThat(error.message()).isEqualTo("Review not allowed");
      assertThat(error.details()).isEqualTo("Only a completed service request can be reviewed; its status is IN_PROGRESS");
      verify(reviews, never()).save(any());
    }

    @Test
    @DisplayName("handle(SubmitReviewCommand) with a score out of range or oversized comment -> VALIDATION_ERROR (AAA)")
    void invalidRating() {
      // Arrange
      asOwner();
      doReturn(Optional.of(completed())).when(requests).findById(REQUEST);
      doReturn(false).when(reviews).existsByServiceRequestId(REQUEST);

      // Act
      var score = failureError(service.handle(new SubmitReviewCommand(REQUEST_ID, USERNAME, 6, 4, 3, null)));
      var nullScore = failureError(service.handle(new SubmitReviewCommand(REQUEST_ID, USERNAME, 5, null, 3, null)));
      var comment = failureError(service.handle(new SubmitReviewCommand(REQUEST_ID, USERNAME, 5, 4, 3, "x".repeat(1001))));

      // Assert
      assertThat(score.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(score.message()).isEqualTo("Validation failed: Review");
      assertThat(score.details()).isEqualTo("Rating communication must be between 1 and 5");
      assertThat(nullScore.details()).isEqualTo("Rating efficacy must be between 1 and 5");
      assertThat(comment.details()).isEqualTo("Review comment must not exceed 1000 characters");
      verify(reviews, never()).save(any());
    }
  }

  @Nested
  class Update {

    private final UpdateReviewCommand command = new UpdateReviewCommand(REVIEW_ID, USERNAME, 1, 2, 3, " Changed ");

    @Test
    @DisplayName("handle(UpdateReviewCommand) inside the edit window -> new rating and comment saved (AAA)")
    void updates() {
      // Arrange
      asOwner();
      var stored = review();
      doReturn(Optional.of(stored)).when(reviews).findById(new ReviewId(REVIEW_ID));
      doAnswer(inv -> inv.getArgument(0)).when(reviews).save(any(Review.class));

      // Act
      var updated = successValue(service.handle(command));

      // Assert
      assertThat(updated.getRating().average()).isEqualTo(2.0);
      assertThat(updated.getComment()).isEqualTo("Changed");
      verify(reviews, times(1)).save(stored);
    }

    @Test
    @DisplayName("handle(UpdateReviewCommand) for a missing, foreign or invalid review id -> REVIEW_NOT_FOUND (AAA)")
    void notFound() {
      // Arrange
      asOwner();
      doReturn(Optional.empty()).when(reviews).findById(new ReviewId(REVIEW_ID));
      doReturn(Optional.of(review())).when(reviews).findById(new ReviewId(41L));
      doReturn(Optional.of(OTHER_OWNER_ID)).when(profiles).fetchOwnerIdByUsername("other");

      // Act
      var missing = failureError(service.handle(command));
      var invalid = failureError(service.handle(new UpdateReviewCommand(0L, USERNAME, 1, 1, 1, null)));
      var nullId = failureError(service.handle(new UpdateReviewCommand(null, USERNAME, 1, 1, 1, null)));
      var foreign = failureError(service.handle(new UpdateReviewCommand(41L, "other", 1, 1, 1, null)));

      // Assert
      assertThat(missing.code()).isEqualTo("REVIEW_NOT_FOUND");
      assertThat(missing.message()).isEqualTo("Review not found: 40");
      assertThat(invalid.code()).isEqualTo("REVIEW_NOT_FOUND");
      assertThat(nullId.code()).isEqualTo("REVIEW_NOT_FOUND");
      assertThat(foreign.code()).isEqualTo("REVIEW_NOT_FOUND");
      verify(reviews, never()).save(any());
    }

    @Test
    @DisplayName("handle(UpdateReviewCommand) after the window closed -> REVIEW_NOT_ALLOWED, nothing saved (AAA)")
    void windowClosed() {
      // Arrange
      asOwner();
      var old = new Review(new ReviewId(REVIEW_ID), new ServiceRequestId(REQUEST_ID), owner(), technician(), rating(), null,
          now().minus(Duration.ofHours(60)), now().minus(Duration.ofHours(12)));
      doReturn(Optional.of(old)).when(reviews).findById(new ReviewId(REVIEW_ID));

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("REVIEW_NOT_ALLOWED");
      assertThat(error.details()).startsWith("The review could only be edited until ");
      verify(reviews, never()).save(any());
      assertThat(old.getRating()).isEqualTo(rating());
    }

    @Test
    @DisplayName("handle(UpdateReviewCommand) with an invalid score -> VALIDATION_ERROR; without owner profile -> ACCESS_DENIED (AAA)")
    void invalid() {
      // Arrange
      asOwner();
      doReturn(Optional.of(review())).when(reviews).findById(new ReviewId(REVIEW_ID));
      doReturn(Optional.empty()).when(profiles).fetchOwnerIdByUsername("nobody");

      // Act
      var score = failureError(service.handle(new UpdateReviewCommand(REVIEW_ID, USERNAME, 0, 1, 1, null)));
      var denied = failureError(service.handle(new UpdateReviewCommand(REVIEW_ID, "nobody", 1, 1, 1, null)));

      // Assert
      assertThat(score.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(denied.code()).isEqualTo("ACCESS_DENIED");
      verify(reviews, never()).save(any());
    }
  }
}
