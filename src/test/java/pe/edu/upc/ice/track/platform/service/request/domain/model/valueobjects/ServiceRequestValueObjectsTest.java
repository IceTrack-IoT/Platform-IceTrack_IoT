package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

class ServiceRequestValueObjectsTest {

  @Nested
  class Identifiers {

    @Test
    @DisplayName("new XxxId(positive) -> exposes the value (AAA)")
    void valid() {
      // Act & Assert
      assertThat(new OwnerId(1L).ownerId()).isEqualTo(1L);
      assertThat(new ReviewId(2L).reviewId()).isEqualTo(2L);
      assertThat(new ServiceRequestId(3L).serviceRequestId()).isEqualTo(3L);
      assertThat(new TechnicianProfileId(4L).technicianProfileId()).isEqualTo(4L);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -100})
    @DisplayName("new XxxId(zero or negative) -> IllegalArgumentException with the identifier's own message (AAA)")
    void nonPositive(long value) {
      // Act & Assert
      assertThatThrownBy(() -> new OwnerId(value)).hasMessage("Owner identifier must be a positive number");
      assertThatThrownBy(() -> new ReviewId(value)).hasMessage("Review identifier must be a positive number");
      assertThatThrownBy(() -> new ServiceRequestId(value)).hasMessage("Service request identifier must be a positive number");
      assertThatThrownBy(() -> new TechnicianProfileId(value)).hasMessage("Technician profile identifier must be a positive number");
    }

    @Test
    @DisplayName("new XxxId(null) -> IllegalArgumentException (AAA)")
    void nullValue() {
      // Act & Assert
      assertThatThrownBy(() -> new OwnerId(null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new ReviewId(null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new ServiceRequestId(null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new TechnicianProfileId(null)).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class ReviewRatingTests {

    @Test
    @DisplayName("new ReviewRating(5, 4, 3) -> average is the arithmetic mean (AAA)")
    void average() {
      // Act & Assert
      assertThat(new ReviewRating(5, 4, 3).average()).isEqualTo(4.0);
      assertThat(new ReviewRating(1, 1, 2).average()).isEqualTo(4.0 / 3.0);
    }

    @ParameterizedTest
    @CsvSource({"1,1,1", "5,5,5"})
    @DisplayName("new ReviewRating(1..5) -> accepted at both limits (AAA)")
    void limits(int c, int e, int p) {
      // Act & Assert
      assertThat(new ReviewRating(c, e, p).communication()).isEqualTo(c);
    }

    @Test
    @DisplayName("new ReviewRating(0 / 6 / null) -> IllegalArgumentException naming the criterion (AAA)")
    void outOfRange() {
      // Act & Assert
      assertThatThrownBy(() -> new ReviewRating(0, 3, 3)).hasMessage("Rating communication must be between 1 and 5");
      assertThatThrownBy(() -> new ReviewRating(3, 6, 3)).hasMessage("Rating efficacy must be between 1 and 5");
      assertThatThrownBy(() -> new ReviewRating(3, 3, null)).hasMessage("Rating performance must be between 1 and 5");
      assertThatThrownBy(() -> new ReviewRating(null, 3, 3)).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class ServiceStatusTests {

    @ParameterizedTest
    @CsvSource({
        "PENDING,PENDING,true", "PENDING,ACCEPTED,true", "PENDING,CANCELED,true", "PENDING,IN_PROGRESS,false", "PENDING,COMPLETED,false",
        "ACCEPTED,PENDING,true", "ACCEPTED,IN_PROGRESS,true", "ACCEPTED,CANCELED,true", "ACCEPTED,COMPLETED,false", "ACCEPTED,ACCEPTED,false",
        "IN_PROGRESS,COMPLETED,true", "IN_PROGRESS,CANCELED,false", "IN_PROGRESS,PENDING,false", "IN_PROGRESS,ACCEPTED,false",
        "COMPLETED,PENDING,false", "COMPLETED,CANCELED,false", "CANCELED,PENDING,false", "CANCELED,ACCEPTED,false"})
    @DisplayName("canTransitionTo(target) -> follows the transition matrix (AAA)")
    void matrix(ServiceStatus from, ServiceStatus to, boolean allowed) {
      // Act & Assert
      assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    @DisplayName("canTransitionTo(null) -> false (AAA)")
    void nullTarget() {
      // Act & Assert
      assertThat(ServiceStatus.PENDING.canTransitionTo(null)).isFalse();
    }

    @Test
    @DisplayName("isTerminal / isActive -> COMPLETED and CANCELED are terminal; others active (AAA)")
    void terminalAndActive() {
      // Act & Assert
      assertThat(ServiceStatus.COMPLETED.isTerminal()).isTrue();
      assertThat(ServiceStatus.CANCELED.isTerminal()).isTrue();
      assertThat(ServiceStatus.PENDING.isTerminal()).isFalse();
      assertThat(ServiceStatus.IN_PROGRESS.isTerminal()).isFalse();
      assertThat(ServiceStatus.PENDING.isActive()).isTrue();
      assertThat(ServiceStatus.ACCEPTED.isActive()).isTrue();
      assertThat(ServiceStatus.IN_PROGRESS.isActive()).isTrue();
      assertThat(ServiceStatus.COMPLETED.isActive()).isFalse();
      assertThat(ServiceStatus.CANCELED.isActive()).isFalse();
    }

    @Test
    @DisplayName("allowedTransitions() -> immutable set of targets (AAA)")
    void allowed() {
      // Act & Assert
      assertThat(ServiceStatus.ACCEPTED.allowedTransitions())
          .containsExactlyInAnyOrder(ServiceStatus.PENDING, ServiceStatus.IN_PROGRESS, ServiceStatus.CANCELED);
      assertThatThrownBy(() -> ServiceStatus.PENDING.allowedTransitions().add(ServiceStatus.COMPLETED))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("enums -> contract values (AAA)")
    void enums() {
      // Act & Assert
      assertThat(ServicePriority.values()).containsExactly(ServicePriority.LOW, ServicePriority.MEDIUM, ServicePriority.HIGH, ServicePriority.URGENT);
      assertThat(ServiceType.values()).containsExactly(ServiceType.PREVENTIVE_MAINTENANCE, ServiceType.REPAIR);
      assertThat(MaintenanceOrigin.values()).containsExactly(MaintenanceOrigin.APP, MaintenanceOrigin.BUTTON);
    }
  }

  @Nested
  class FilterAndPage {

    @Test
    @DisplayName("ServiceRequestFilter.NONE -> no criteria (AAA)")
    void none() {
      // Act & Assert
      assertThat(ServiceRequestFilter.NONE.statuses()).isEmpty();
      assertThat(ServiceRequestFilter.NONE.types()).isEmpty();
      assertThat(ServiceRequestFilter.NONE.priorities()).isEmpty();
      assertThat(ServiceRequestFilter.NONE.equipmentId()).isNull();
      assertThat(ServiceRequestFilter.NONE.createdFrom()).isNull();
    }

    @Test
    @DisplayName("new ServiceRequestFilter(null sets) -> empty immutable copies (AAA)")
    void copies() {
      // Act
      var filter = new ServiceRequestFilter(null, 1L, 2L, null, null, null, null, 9L);

      // Assert
      assertThat(filter.statuses()).isEmpty();
      assertThat(filter.sourceAlertId()).isEqualTo(9L);
      assertThatThrownBy(() -> filter.types().add(ServiceType.REPAIR)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("new ServiceRequestFilter(from after to) -> IllegalArgumentException; equal bounds accepted (AAA)")
    void range() {
      // Arrange
      var from = Instant.parse("2026-01-02T00:00:00Z");
      var to = Instant.parse("2026-01-01T00:00:00Z");

      // Act & Assert
      assertThatThrownBy(() -> new ServiceRequestFilter(null, null, null, null, null, from, to, null))
          .hasMessage("Parameter 'created_from' must not be after 'created_to'");
      assertThat(new ServiceRequestFilter(null, null, null, null, null, from, from, null).createdFrom()).isEqualTo(from);
    }

    @Test
    @DisplayName("new ServiceRequestPage(content, page, size, total) -> immutable copy of the content (AAA)")
    void page() {
      // Arrange
      var content = new java.util.ArrayList<>(List.of(pending()));

      // Act
      var page = new ServiceRequestPage(content, 0, 10, 1);
      content.clear();

      // Assert
      assertThat(page.content()).hasSize(1);
      assertThatThrownBy(() -> page.content().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("new ServiceRequestPage(invalid) -> IllegalArgumentException (AAA)")
    void invalidPage() {
      // Act & Assert
      assertThatThrownBy(() -> new ServiceRequestPage(null, 0, 1, 0)).hasMessage("Page content must not be null");
      assertThatThrownBy(() -> new ServiceRequestPage(List.of(), -1, 1, 0)).hasMessage("Page index must not be negative");
      assertThatThrownBy(() -> new ServiceRequestPage(List.of(), 0, 0, 0)).hasMessage("Page size must be a positive number of rows");
    }

    @ParameterizedTest
    @CsvSource({"0,10,1", "10,10,1", "11,10,2", "95,10,10", "101,100,2"})
    @DisplayName("totalPages() -> ceil(total/size), at least 1 (AAA)")
    void totalPages(long total, int size, int expected) {
      // Act & Assert
      assertThat(new ServiceRequestPage(List.of(), 0, size, total).totalPages()).isEqualTo(expected);
    }

    @Test
    @DisplayName("ServiceRequestPage.empty(page, size) -> no rows, zero total (AAA)")
    void empty() {
      // Act
      var page = ServiceRequestPage.empty(3, 15);

      // Assert
      assertThat(page.content()).isEmpty();
      assertThat(page.page()).isEqualTo(3);
      assertThat(page.size()).isEqualTo(15);
      assertThat(page.totalElements()).isZero();
    }
  }

  @Nested
  class DisplayDetails {

    @Test
    @DisplayName("ServiceRequestDisplayDetails.NONE and null maps -> empty collections (AAA)")
    void defaults() {
      // Act
      var details = new ServiceRequestDisplayDetails(null, null, null, null);

      // Assert
      assertThat(details.equipmentNames()).isEmpty();
      assertThat(details.reviewedServiceRequestIds()).isEmpty();
      assertThat(ServiceRequestDisplayDetails.NONE.equipmentNameOf(pending())).isNull();
    }

    @Test
    @DisplayName("equipmentNameOf / siteNameOf / technicianNameOf / isReviewed -> look the request's ids up (AAA)")
    void lookups() {
      // Arrange
      var details = new ServiceRequestDisplayDetails(Map.of(EQUIPMENT_ID, "Freezer 1"), Map.of(SITE_ID, "Lima Norte"),
          Map.of(TECHNICIAN_ID, "Luis Rojas"), Set.of(REQUEST_ID));

      // Act & Assert
      assertThat(details.equipmentNameOf(pending())).isEqualTo("Freezer 1");
      assertThat(details.siteNameOf(pending())).isEqualTo("Lima Norte");
      assertThat(details.technicianNameOf(assigned())).isEqualTo("Luis Rojas");
      assertThat(details.technicianNameOf(pending())).isNull();
      assertThat(details.isReviewed(pending())).isTrue();
    }

    @Test
    @DisplayName("isReviewed(request) -> false when not in the set or the request has no id (AAA)")
    void notReviewed() {
      // Arrange
      var details = new ServiceRequestDisplayDetails(null, null, null, Set.of(1L));
      var unsaved = pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest.create(owner(),
          new pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId(1L),
          new pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId(1L), ServiceType.REPAIR,
          ServicePriority.LOW, "d", null);

      // Act & Assert
      assertThat(details.isReviewed(pending())).isFalse();
      assertThat(details.isReviewed(unsaved)).isFalse();
    }

    @Test
    @DisplayName("ServiceRequestDisplayDetails -> defensively copies the maps (AAA)")
    void copies() {
      // Arrange
      var names = new java.util.HashMap<Long, String>();
      names.put(1L, "A");

      // Act
      var details = new ServiceRequestDisplayDetails(names, null, null, null);
      names.put(2L, "B");

      // Assert
      assertThat(details.equipmentNames()).containsOnlyKeys(1L);
    }
  }

  @Nested
  class Wrappers {

    @Test
    @DisplayName("AcceptedServiceRequest / IssuedEdgeGrant -> require every component (AAA)")
    void required() {
      // Arrange
      var grant = new IssuedEdgeGrant(grant(), "it_grt_x");

      // Act & Assert
      assertThat(new AcceptedServiceRequest(accepted(), grant).edgeGrant()).isSameAs(grant);
      assertThatThrownBy(() -> new AcceptedServiceRequest(null, grant)).isInstanceOf(NullPointerException.class)
          .hasMessage("serviceRequest must not be null");
      assertThatThrownBy(() -> new AcceptedServiceRequest(accepted(), null)).hasMessage("edgeGrant must not be null");
      assertThatThrownBy(() -> new IssuedEdgeGrant(null, "t")).hasMessage("grant must not be null");
      assertThatThrownBy(() -> new IssuedEdgeGrant(grant(), null)).hasMessage("token must not be null");
    }

    @Test
    @DisplayName("InterventionRegistration -> requires a request with an intervention and exposes it (AAA)")
    void registration() {
      // Arrange
      var done = completed();

      // Act
      var registration = new InterventionRegistration(done, true);

      // Assert
      assertThat(registration.intervention()).isSameAs(done.getIntervention());
      assertThat(registration.replayed()).isTrue();
      assertThatThrownBy(() -> new InterventionRegistration(null, false)).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("An intervention registration requires a request holding an intervention");
      assertThatThrownBy(() -> new InterventionRegistration(inProgress(), false)).isInstanceOf(IllegalArgumentException.class);
    }
  }
}
