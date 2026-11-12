package pe.edu.upc.ice.track.platform.profiles.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.ProjectedReviewScore;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianMetrics;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianMetricsRepository;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ReviewSubmittedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ReviewUpdatedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ServiceRequestCompletedIntegrationEvent;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OWNER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.TECHNICIAN_ID;

@ExtendWith(MockitoExtension.class)
class TechnicianMetricsProjectorTest {

  private static final Long REQUEST_ID = 77L;
  private static final Long REVIEW_ID = 500L;
  private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

  @Mock
  private TechnicianMetricsRepository repository;

  @InjectMocks
  private TechnicianMetricsProjector projector;

  private TechnicianMetrics savedMetrics() {
    var captor = ArgumentCaptor.forClass(TechnicianMetrics.class);
    verify(repository, times(1)).save(captor.capture());
    return captor.getValue();
  }

  private ProjectedReviewScore savedScore() {
    var captor = ArgumentCaptor.forClass(ProjectedReviewScore.class);
    verify(repository, times(1)).saveReviewScore(captor.capture());
    return captor.getValue();
  }

  @Nested
  class ServiceCompleted {

    private ServiceRequestCompletedIntegrationEvent event(Long requestId, Long technicianId) {
      return new ServiceRequestCompletedIntegrationEvent(requestId, OWNER_ID, technicianId, 10L, T0);
    }

    @Test
    @DisplayName("on(ServiceRequestCompleted) first time -> counts it on top of the existing metrics (AAA)")
    void countsOnce() {
      // Arrange
      when(repository.registerCompletedService(REQUEST_ID, TECHNICIAN_ID)).thenReturn(true);
      when(repository.findByTechnicianProfileId(TECHNICIAN_ID))
          .thenReturn(Optional.of(new TechnicianMetrics(TECHNICIAN_ID, 4, 0, 0, 1L)));

      // Act
      projector.on(event(REQUEST_ID, TECHNICIAN_ID));

      // Assert
      assertThat(savedMetrics().getCompletedServicesCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("on(ServiceRequestCompleted) technician without metrics yet -> starts from empty (AAA)")
    void startsFromEmpty() {
      // Arrange
      when(repository.registerCompletedService(REQUEST_ID, TECHNICIAN_ID)).thenReturn(true);
      when(repository.findByTechnicianProfileId(TECHNICIAN_ID)).thenReturn(Optional.empty());

      // Act
      projector.on(event(REQUEST_ID, TECHNICIAN_ID));

      // Assert
      var metrics = savedMetrics();
      assertThat(metrics.getTechnicianProfileId()).isEqualTo(TECHNICIAN_ID);
      assertThat(metrics.getCompletedServicesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("on(ServiceRequestCompleted) already counted (redelivery) -> nothing is saved (AAA)")
    void duplicateIsIgnored() {
      // Arrange
      when(repository.registerCompletedService(REQUEST_ID, TECHNICIAN_ID)).thenReturn(false);

      // Act
      projector.on(event(REQUEST_ID, TECHNICIAN_ID));

      // Assert
      verify(repository, never()).save(any());
      verify(repository, never()).findByTechnicianProfileId(any());
    }

    @Test
    @DisplayName("on(ServiceRequestCompleted) without request or technician -> ignored (AAA)")
    void incompleteIsIgnored() {
      // Act
      projector.on(event(null, TECHNICIAN_ID));
      projector.on(event(REQUEST_ID, null));

      // Assert
      verifyNoInteractions(repository);
    }
  }

  @Nested
  class ReviewSubmitted {

    private ReviewSubmittedIntegrationEvent event(Long reviewId, Long technicianId, Double rating, Instant at) {
      return new ReviewSubmittedIntegrationEvent(reviewId, REQUEST_ID, technicianId, rating, at);
    }

    @Test
    @DisplayName("on(ReviewSubmitted) new review -> adds the rating and remembers the score (AAA)")
    void addsRating() {
      // Arrange
      when(repository.findReviewScore(REVIEW_ID)).thenReturn(Optional.empty());
      when(repository.findByTechnicianProfileId(TECHNICIAN_ID))
          .thenReturn(Optional.of(new TechnicianMetrics(TECHNICIAN_ID, 3, 4.0, 1, null)));

      // Act
      projector.on(event(REVIEW_ID, TECHNICIAN_ID, 5.0, T0));

      // Assert
      var metrics = savedMetrics();
      assertThat(metrics.getRatingCount()).isEqualTo(2);
      assertThat(metrics.getAverageRating()).isCloseTo(4.5, within(1e-9));
      assertThat(savedScore()).isEqualTo(new ProjectedReviewScore(REVIEW_ID, TECHNICIAN_ID, 5.0, T0));
    }

    @Test
    @DisplayName("on(ReviewSubmitted) without timestamp -> the score is stamped with the current time (AAA)")
    void defaultsTimestamp() {
      // Arrange
      var before = Instant.now();
      when(repository.findReviewScore(REVIEW_ID)).thenReturn(Optional.empty());
      when(repository.findByTechnicianProfileId(TECHNICIAN_ID)).thenReturn(Optional.empty());

      // Act
      projector.on(event(REVIEW_ID, TECHNICIAN_ID, 3.0, null));

      // Assert
      assertThat(savedScore().scoredAt()).isBetween(before, Instant.now().plusSeconds(1));
      assertThat(savedMetrics().getAverageRating()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("on(ReviewSubmitted) review already counted -> ignored (AAA)")
    void duplicateIsIgnored() {
      // Arrange
      when(repository.findReviewScore(REVIEW_ID))
          .thenReturn(Optional.of(new ProjectedReviewScore(REVIEW_ID, TECHNICIAN_ID, 5.0, T0)));

      // Act
      projector.on(event(REVIEW_ID, TECHNICIAN_ID, 5.0, T0));

      // Assert
      verify(repository, never()).save(any());
      verify(repository, never()).saveReviewScore(any());
    }

    @Test
    @DisplayName("on(ReviewSubmitted) with null ids or non-finite rating -> ignored (AAA)")
    void incompleteIsIgnored() {
      // Act
      projector.on(event(null, TECHNICIAN_ID, 5.0, T0));
      projector.on(event(REVIEW_ID, null, 5.0, T0));
      projector.on(event(REVIEW_ID, TECHNICIAN_ID, null, T0));
      projector.on(event(REVIEW_ID, TECHNICIAN_ID, Double.NaN, T0));
      projector.on(event(REVIEW_ID, TECHNICIAN_ID, Double.POSITIVE_INFINITY, T0));

      // Assert
      verifyNoInteractions(repository);
    }
  }

  @Nested
  class ReviewUpdated {

    private ReviewUpdatedIntegrationEvent event(Double rating, Instant at) {
      return new ReviewUpdatedIntegrationEvent(REVIEW_ID, REQUEST_ID, TECHNICIAN_ID, rating, at);
    }

    @Test
    @DisplayName("on(ReviewUpdated) newer edit of a counted review -> replaces the old score, count unchanged (AAA)")
    void replacesScore() {
      // Arrange
      when(repository.findReviewScore(REVIEW_ID))
          .thenReturn(Optional.of(new ProjectedReviewScore(REVIEW_ID, TECHNICIAN_ID, 4.0, T0)));
      when(repository.findByTechnicianProfileId(TECHNICIAN_ID))
          .thenReturn(Optional.of(new TechnicianMetrics(TECHNICIAN_ID, 0, 9.0, 2, null)));

      // Act
      projector.on(event(2.0, T0.plusSeconds(60)));

      // Assert
      var metrics = savedMetrics();
      assertThat(metrics.getRatingCount()).isEqualTo(2);
      assertThat(metrics.getAverageRating()).isCloseTo(3.5, within(1e-9));
      assertThat(savedScore()).isEqualTo(new ProjectedReviewScore(REVIEW_ID, TECHNICIAN_ID, 2.0, T0.plusSeconds(60)));
    }

    @Test
    @DisplayName("on(ReviewUpdated) edit for a review not seen before -> counted as a new rating (AAA)")
    void unseenReviewIsAdded() {
      // Arrange
      when(repository.findReviewScore(REVIEW_ID)).thenReturn(Optional.empty());
      when(repository.findByTechnicianProfileId(TECHNICIAN_ID)).thenReturn(Optional.empty());

      // Act
      projector.on(event(4.0, T0));

      // Assert
      assertThat(savedMetrics().getRatingCount()).isEqualTo(1);
      assertThat(savedScore().score()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("on(ReviewUpdated) edit not newer than the counted score (late or equal) -> ignored (AAA)")
    void staleEditIsIgnored() {
      // Arrange
      when(repository.findReviewScore(REVIEW_ID))
          .thenReturn(Optional.of(new ProjectedReviewScore(REVIEW_ID, TECHNICIAN_ID, 4.0, T0)));

      // Act
      projector.on(event(1.0, T0));
      projector.on(event(1.0, T0.minusSeconds(5)));

      // Assert
      verify(repository, never()).save(any());
      verify(repository, never()).saveReviewScore(any());
    }

    @Test
    @DisplayName("on(ReviewUpdated) without timestamp on the edit -> applied (cannot be proven stale) (AAA)")
    void editWithoutTimestampIsApplied() {
      // Arrange
      when(repository.findReviewScore(REVIEW_ID))
          .thenReturn(Optional.of(new ProjectedReviewScore(REVIEW_ID, TECHNICIAN_ID, 4.0, T0)));
      when(repository.findByTechnicianProfileId(TECHNICIAN_ID))
          .thenReturn(Optional.of(new TechnicianMetrics(TECHNICIAN_ID, 0, 4.0, 1, null)));

      // Act
      projector.on(event(5.0, null));

      // Assert
      assertThat(savedMetrics().getAverageRating()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("on(ReviewUpdated) with null ids or non-finite rating -> ignored (AAA)")
    void incompleteIsIgnored() {
      // Act
      projector.on(new ReviewUpdatedIntegrationEvent(null, REQUEST_ID, TECHNICIAN_ID, 4.0, T0));
      projector.on(new ReviewUpdatedIntegrationEvent(REVIEW_ID, REQUEST_ID, null, 4.0, T0));
      projector.on(event(null, T0));
      projector.on(event(Double.NaN, T0));

      // Assert
      verifyNoInteractions(repository);
    }
  }
}
