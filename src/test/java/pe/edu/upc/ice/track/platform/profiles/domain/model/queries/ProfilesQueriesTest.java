package pe.edu.upc.ice.track.platform.profiles.domain.model.queries;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TechnicianDirectorySort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfilesQueriesTest {

  @Test
  @DisplayName("new GetPublicTechnicianDirectoryQuery(blank text, null sort) -> normalized to null and NAME (AAA)")
  void normalizesOptionalCriteria() {
    // Act
    var query = new GetPublicTechnicianDirectoryQuery("   ", "", null, null, 0, 20);

    // Assert
    assertThat(query.search()).isNull();
    assertThat(query.speciality()).isNull();
    assertThat(query.sort()).isEqualTo(TechnicianDirectorySort.NAME);
    assertThat(query.minRating()).isNull();
  }

  @Test
  @DisplayName("new GetPublicTechnicianDirectoryQuery(padded text) -> stripped (AAA)")
  void stripsText() {
    // Act
    var query = new GetPublicTechnicianDirectoryQuery("  ana ", " HVAC ", 4.5, TechnicianDirectorySort.RATING, 1, 10);

    // Assert
    assertThat(query.search()).isEqualTo("ana");
    assertThat(query.speciality()).isEqualTo("HVAC");
    assertThat(query.minRating()).isEqualTo(4.5);
    assertThat(query.sort()).isEqualTo(TechnicianDirectorySort.RATING);
  }

  @Test
  @DisplayName("new GetPublicTechnicianDirectoryQuery(size above 100) -> capped at 100 (AAA)")
  void capsSize() {
    // Act & Assert
    assertThat(new GetPublicTechnicianDirectoryQuery(null, null, null, null, 0, 101).size())
        .isEqualTo(GetPublicTechnicianDirectoryQuery.MAX_PAGE_SIZE);
    assertThat(new GetPublicTechnicianDirectoryQuery(null, null, null, null, 0, 100).size()).isEqualTo(100);
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 5.1, Double.NaN})
  @DisplayName("new GetPublicTechnicianDirectoryQuery(min rating outside 0..5 or NaN) -> IllegalArgumentException (AAA)")
  void rejectsBadRating(double rating) {
    // Act & Assert
    assertThatThrownBy(() -> new GetPublicTechnicianDirectoryQuery(null, null, rating, null, 0, 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Parameter 'min_rating' must be between 0 and 5");
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 5.0})
  @DisplayName("new GetPublicTechnicianDirectoryQuery(rating at the limits) -> accepted (AAA)")
  void acceptsRatingLimits(double rating) {
    // Act & Assert
    assertThat(new GetPublicTechnicianDirectoryQuery(null, null, rating, null, 0, 10).minRating()).isEqualTo(rating);
  }

  @Test
  @DisplayName("new GetPublicTechnicianDirectoryQuery(negative page or size below 1) -> IllegalArgumentException (AAA)")
  void rejectsBadPaging() {
    // Act & Assert
    assertThatThrownBy(() -> new GetPublicTechnicianDirectoryQuery(null, null, null, null, -1, 10))
        .hasMessage("Parameter 'page' must not be negative");
    assertThatThrownBy(() -> new GetPublicTechnicianDirectoryQuery(null, null, null, null, 0, 0))
        .hasMessage("Parameter 'size' must be at least 1");
  }
}
