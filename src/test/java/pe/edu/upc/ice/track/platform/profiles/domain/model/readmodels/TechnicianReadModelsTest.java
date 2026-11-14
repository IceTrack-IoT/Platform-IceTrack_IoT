package pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.TECHNICIAN_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedTechnician;

class TechnicianReadModelsTest {

  @Nested
  class TechnicianMetricsTest {

    @Test
    @DisplayName("TechnicianMetrics.empty(id) -> zero counters and no rating available (AAA)")
    void emptyMetrics() {
      // Act
      var metrics = TechnicianMetrics.empty(TECHNICIAN_ID);

      // Assert
      assertThat(metrics.getTechnicianProfileId()).isEqualTo(TECHNICIAN_ID);
      assertThat(metrics.getCompletedServicesCount()).isZero();
      assertThat(metrics.getRatingCount()).isZero();
      assertThat(metrics.getAverageRating()).isNull();
      assertThat(metrics.isRatingAvailable()).isFalse();
      assertThat(metrics.getVersion()).isNull();
    }

    @Test
    @DisplayName("new TechnicianMetrics(counters) -> average computed from the stored sum and count (AAA)")
    void averageFromStoredValues() {
      // Act
      var metrics = new TechnicianMetrics(TECHNICIAN_ID, 4, 18.0, 4, 2L);

      // Assert
      assertThat(metrics.getAverageRating()).isEqualTo(4.5);
      assertThat(metrics.isRatingAvailable()).isTrue();
      assertThat(metrics.getVersion()).isEqualTo(2L);
    }

    @Test
    @DisplayName("new TechnicianMetrics(invalid) -> null id and negative counters rejected (AAA)")
    void invalidConstruction() {
      // Act & Assert
      assertThatThrownBy(() -> new TechnicianMetrics(null, 0, 0, 0, null))
          .isInstanceOf(NullPointerException.class).hasMessage("technicianProfileId must not be null");
      assertThatThrownBy(() -> new TechnicianMetrics(TECHNICIAN_ID, -1, 0, 0, null))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("Technician metrics counters must not be negative");
      assertThatThrownBy(() -> new TechnicianMetrics(TECHNICIAN_ID, 0, 0, -1, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("recordCompletedService() -> increments only the completed counter (AAA)")
    void recordCompletedService() {
      // Arrange
      var metrics = TechnicianMetrics.empty(TECHNICIAN_ID);

      // Act
      metrics.recordCompletedService();
      metrics.recordCompletedService();

      // Assert
      assertThat(metrics.getCompletedServicesCount()).isEqualTo(2);
      assertThat(metrics.getRatingCount()).isZero();
    }

    @Test
    @DisplayName("addRating(score) -> updates sum, count and the running average (AAA)")
    void addRating() {
      // Arrange
      var metrics = TechnicianMetrics.empty(TECHNICIAN_ID);

      // Act
      metrics.addRating(5.0);
      metrics.addRating(4.0);

      // Assert
      assertThat(metrics.getRatingCount()).isEqualTo(2);
      assertThat(metrics.getRatingSum()).isEqualTo(9.0);
      assertThat(metrics.getAverageRating()).isCloseTo(4.5, within(1e-9));
    }

    @Test
    @DisplayName("replaceRating(old, new) -> adjusts the sum without changing the count (AAA)")
    void replaceRating() {
      // Arrange
      var metrics = new TechnicianMetrics(TECHNICIAN_ID, 0, 9.0, 2, null);

      // Act
      metrics.replaceRating(4.0, 2.0);

      // Assert
      assertThat(metrics.getRatingCount()).isEqualTo(2);
      assertThat(metrics.getAverageRating()).isCloseTo(3.5, within(1e-9));
    }

    @Test
    @DisplayName("replaceRating(old, new) with no ratings yet -> behaves as addRating(new) (AAA)")
    void replaceRatingWithoutPrevious() {
      // Arrange
      var metrics = TechnicianMetrics.empty(TECHNICIAN_ID);

      // Act
      metrics.replaceRating(3.0, 5.0);

      // Assert
      assertThat(metrics.getRatingCount()).isEqualTo(1);
      assertThat(metrics.getAverageRating()).isEqualTo(5.0);
    }
  }

  @Nested
  class TechnicianDirectoryEntryTest {

    @Test
    @DisplayName("new TechnicianDirectoryEntry(profile, null metrics) -> zero services and no rating (AAA)")
    void withoutMetrics() {
      // Act
      var entry = new TechnicianDirectoryEntry(persistedTechnician(), null);

      // Assert
      assertThat(entry.fullName()).isEqualTo("Ana Torres");
      assertThat(entry.specialityName()).isEqualTo("Refrigeration");
      assertThat(entry.completedServices()).isZero();
      assertThat(entry.averageRating()).isNull();
      assertThat(entry.findMetrics()).isEmpty();
    }

    @Test
    @DisplayName("new TechnicianDirectoryEntry(profile, metrics) -> delegates to the metrics (AAA)")
    void withMetrics() {
      // Arrange
      var metrics = new TechnicianMetrics(TECHNICIAN_ID, 7, 8.0, 2, null);

      // Act
      var entry = new TechnicianDirectoryEntry(persistedTechnician(), metrics);

      // Assert
      assertThat(entry.completedServices()).isEqualTo(7);
      assertThat(entry.averageRating()).isEqualTo(4.0);
      assertThat(entry.findMetrics()).containsSame(metrics);
    }

    @Test
    @DisplayName("new TechnicianDirectoryEntry(null profile) -> NullPointerException (AAA)")
    void nullProfile() {
      // Act & Assert
      assertThatThrownBy(() -> new TechnicianDirectoryEntry(null, null)).hasMessage("profile must not be null");
    }
  }

  @Nested
  class TechnicianDirectoryPageTest {

    @Test
    @DisplayName("totalPages() -> ceil(total / size), at least one page (AAA)")
    void totalPages() {
      // Act & Assert
      assertThat(new TechnicianDirectoryPage(List.of(), 0, 10, 0).totalPages()).isEqualTo(1);
      assertThat(new TechnicianDirectoryPage(List.of(), 0, 10, 10).totalPages()).isEqualTo(1);
      assertThat(new TechnicianDirectoryPage(List.of(), 0, 10, 11).totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("new TechnicianDirectoryPage(invalid) -> IllegalArgumentException (AAA)")
    void invalid() {
      // Act & Assert
      assertThatThrownBy(() -> new TechnicianDirectoryPage(null, 0, 10, 0)).hasMessage("Page content must not be null");
      assertThatThrownBy(() -> new TechnicianDirectoryPage(List.of(), -1, 10, 0))
          .hasMessage("Page index must not be negative");
      assertThatThrownBy(() -> new TechnicianDirectoryPage(List.of(), 0, 0, 0))
          .hasMessage("Page size must be a positive number of rows");
    }

    @Test
    @DisplayName("new TechnicianDirectoryPage(content) -> content is an immutable copy (AAA)")
    void immutableContent() {
      // Arrange
      var page = new TechnicianDirectoryPage(List.of(new TechnicianDirectoryEntry(persistedTechnician(), null)), 0, 10, 1);

      // Act & Assert
      assertThatThrownBy(() -> page.content().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
