package pe.edu.upc.ice.track.platform.profiles.application.internal.queryservices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetPublicTechnicianDirectoryQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianDirectoryEntry;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianMetrics;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TechnicianDirectorySort;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianMetricsRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianProfileRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.technicianNamed;

/** Directory listing: filters, sort orders and paging of {@link TechnicianQueryServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class TechnicianDirectoryQueryTest {

  private static final TechnicianProfile ANA = technicianNamed(1L, 11L, "Ana", "Torres", "Refrigeration");
  private static final TechnicianProfile BETO = technicianNamed(2L, 12L, "Beto", "Alvarez", "HVAC");
  private static final TechnicianProfile CARLA = technicianNamed(3L, 13L, "carla", "Ruiz", "Refrigeration");
  private static final TechnicianProfile ANA_TWIN = technicianNamed(4L, 14L, "Ana", "Torres", "HVAC");

  @Mock
  private TechnicianProfileRepository technicianProfileRepository;
  @Mock
  private TechnicianMetricsRepository technicianMetricsRepository;

  @InjectMocks
  private TechnicianQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    lenient().when(technicianProfileRepository.findAll()).thenReturn(List.of(CARLA, BETO, ANA_TWIN, ANA));
    lenient().when(technicianMetricsRepository.findByTechnicianProfileIds(List.of(3L, 2L, 4L, 1L))).thenReturn(List.of(
        new TechnicianMetrics(1L, 5, 8.0, 2, null),
        new TechnicianMetrics(2L, 10, 5.0, 1, null)));
  }

  private static GetPublicTechnicianDirectoryQuery query(String search, String speciality, Double minRating,
                                                         TechnicianDirectorySort sort, int page, int size) {
    return new GetPublicTechnicianDirectoryQuery(search, speciality, minRating, sort, page, size);
  }

  private List<Long> idsOf(GetPublicTechnicianDirectoryQuery query) {
    return service.handle(query).content().stream()
        .map(TechnicianDirectoryEntry::profile).map(TechnicianProfile::getUserProfileId).toList();
  }

  @Test
  @DisplayName("handle(directory, NAME) -> case-insensitive by full name, ties by id (AAA)")
  void sortByName() {
    // Act & Assert
    assertThat(idsOf(query(null, null, null, TechnicianDirectorySort.NAME, 0, 10))).containsExactly(1L, 4L, 2L, 3L);
  }

  @Test
  @DisplayName("handle(directory, no sort given) -> defaults to NAME (AAA)")
  void defaultSortIsName() {
    // Act & Assert
    assertThat(idsOf(query(null, null, null, null, 0, 10))).containsExactly(1L, 4L, 2L, 3L);
  }

  @Test
  @DisplayName("handle(directory, RATING) -> best rating first, unrated last ordered by name (AAA)")
  void sortByRating() {
    // Act & Assert
    assertThat(idsOf(query(null, null, null, TechnicianDirectorySort.RATING, 0, 10))).containsExactly(2L, 1L, 4L, 3L);
  }

  @Test
  @DisplayName("handle(directory, COMPLETED_SERVICES) -> most services first, ties by name (AAA)")
  void sortByCompletedServices() {
    // Act & Assert
    assertThat(idsOf(query(null, null, null, TechnicianDirectorySort.COMPLETED_SERVICES, 0, 10)))
        .containsExactly(2L, 1L, 4L, 3L);
  }

  @Test
  @DisplayName("handle(directory, search) -> matches name or speciality, ignoring case (AAA)")
  void searchMatchesNameOrSpeciality() {
    // Act & Assert
    assertThat(idsOf(query("ANA", null, null, null, 0, 10))).containsExactly(1L, 4L);
    assertThat(idsOf(query("hvac", null, null, null, 0, 10))).containsExactly(4L, 2L);
    assertThat(idsOf(query("zzz", null, null, null, 0, 10))).isEmpty();
  }

  @Test
  @DisplayName("handle(directory, speciality) -> exact speciality ignoring case, not a substring (AAA)")
  void specialityFilterIsExact() {
    // Act & Assert
    assertThat(idsOf(query(null, "refrigeration", null, null, 0, 10))).containsExactly(1L, 3L);
    assertThat(idsOf(query(null, "refrig", null, null, 0, 10))).isEmpty();
  }

  @Test
  @DisplayName("handle(directory, minRating) -> only rated technicians at or above it (AAA)")
  void minRatingFilter() {
    // Act & Assert
    assertThat(idsOf(query(null, null, 4.0, null, 0, 10))).containsExactly(1L, 2L);
    assertThat(idsOf(query(null, null, 4.5, null, 0, 10))).containsExactly(2L);
    assertThat(idsOf(query(null, null, 0.0, null, 0, 10))).containsExactly(1L, 2L);
  }

  @Test
  @DisplayName("handle(directory, search + speciality + minRating) -> all criteria combined (AAA)")
  void combinedFilters() {
    // Act & Assert
    assertThat(idsOf(query("torres", "Refrigeration", 3.0, null, 0, 10))).containsExactly(1L);
  }

  @Test
  @DisplayName("handle(directory, page 1 of size 2) -> second slice with page metadata (AAA)")
  void secondPage() {
    // Act
    var page = service.handle(query(null, null, null, null, 1, 2));

    // Assert
    assertThat(page.content()).extracting(entry -> entry.profile().getUserProfileId()).containsExactly(2L, 3L);
    assertThat(page.page()).isEqualTo(1);
    assertThat(page.size()).isEqualTo(2);
    assertThat(page.totalElements()).isEqualTo(4L);
    assertThat(page.totalPages()).isEqualTo(2);
  }

  @Test
  @DisplayName("handle(directory, last page partially filled) -> only the remaining entries (AAA)")
  void partialLastPage() {
    // Act & Assert
    assertThat(idsOf(query(null, null, null, null, 1, 3))).containsExactly(3L);
  }

  @Test
  @DisplayName("handle(directory, page beyond the end) -> empty content, total still reported (AAA)")
  void pageBeyondTheEnd() {
    // Act
    var page = service.handle(query(null, null, null, null, 5, 10));

    // Assert
    assertThat(page.content()).isEmpty();
    assertThat(page.totalElements()).isEqualTo(4L);
  }

  @Test
  @DisplayName("handle(directory) -> entries expose metrics only for rated technicians (AAA)")
  void entriesCarryMetrics() {
    // Act
    var content = service.handle(query(null, null, null, TechnicianDirectorySort.RATING, 0, 10)).content();

    // Assert
    assertThat(content.get(0).completedServices()).isEqualTo(10);
    assertThat(content.get(0).averageRating()).isEqualTo(5.0);
    assertThat(content.get(3).findMetrics()).isEmpty();
  }

  @Test
  @DisplayName("handle(directory) with no technicians -> empty page with one total page (AAA)")
  void noTechnicians() {
    // Arrange
    lenient().when(technicianProfileRepository.findAll()).thenReturn(List.of());
    lenient().when(technicianMetricsRepository.findByTechnicianProfileIds(List.of())).thenReturn(List.of());

    // Act
    var page = service.handle(query(null, null, null, null, 0, 10));

    // Assert
    assertThat(page.content()).isEmpty();
    assertThat(page.totalPages()).isEqualTo(1);
  }
}
