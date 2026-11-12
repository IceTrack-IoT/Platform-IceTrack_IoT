package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalAssetsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalProfilesServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalServiceRequestsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalServiceRequestsServiceForReporting.ServiceIntervention;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportSection;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingCommandFixtures.*;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ServiceInterventionStrategyTest {

  @Mock private ExternalServiceRequestsServiceForReporting serviceRequests;
  @Mock private ExternalProfilesServiceForReporting profiles;
  @Mock private ExternalAssetsServiceForReporting assets;
  @InjectMocks private ServiceInterventionStrategy strategy;

  private void request(ServiceIntervention intervention) {
    doReturn(Optional.of(intervention)).when(serviceRequests).fetchServiceIntervention(REQUEST_ID);
  }

  private static String row(ReportSection section, String label) {
    return section.rows().stream().filter(r -> r.label().equals(label)).findFirst().orElseThrow().value();
  }

  private static ServiceIntervention sparse(ServiceIntervention.Work work, ServiceIntervention.Rating rating,
                                           List<ServiceIntervention.StatusChange> history, Long technician) {
    return new ServiceIntervention(REQUEST_ID, OWNER_ID, EQUIPMENT_ID, SITE_ID, technician, "PREVENTIVE", "LOW",
        "COMPLETED", "Routine", CREATED, null, null, history, work, rating);
  }

  @Nested
  class Kpis {

    @Test
    @DisplayName("generate(filters) -> work and review KPIs of the completed request (AAA)")
    void full() {
      // Arrange
      request(intervention(OWNER_ID, "COMPLETED"));

      // Act
      var kpis = strategy.generate(requestFilters());

      // Assert
      assertThat(kpis).extracting(KpiValue::key).containsExactly(
          "intervention_duration_min", "spare_parts_count", "photo_count",
          "review_average", "review_communication", "review_efficacy", "review_performance");
      assertThat(kpis).extracting(KpiValue::value).containsExactly(120.0, 1.0, 1.0, 4.0, 5.0, 4.0, 3.0);
    }

    @Test
    @DisplayName("generate(filters) for a request without work or review -> no KPIs (AAA)")
    void empty() {
      // Arrange
      request(sparse(null, null, List.of(), null));

      // Act & Assert
      assertThat(strategy.generate(requestFilters())).isEmpty();
    }

    @Test
    @DisplayName("generate(filters) -> IllegalStateException for null filters request, missing or non-completed request (AAA)")
    void guards() {
      // Arrange
      doReturn(Optional.empty()).when(serviceRequests).fetchServiceIntervention(REQUEST_ID);

      // Act & Assert
      assertThatThrownBy(() -> strategy.generate(null)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> strategy.generate(siteFilters()))
          .isInstanceOf(IllegalStateException.class).hasMessage("A service intervention report needs a service request");
      assertThatThrownBy(() -> strategy.generate(requestFilters()))
          .isInstanceOf(IllegalStateException.class).hasMessage("Service request 77 no longer exists");
    }

    @Test
    @DisplayName("generate(filters) for a request that is not completed -> IllegalStateException (AAA)")
    void notCompleted() {
      // Arrange
      request(intervention(OWNER_ID, "IN_PROGRESS"));

      // Act & Assert
      assertThatThrownBy(() -> strategy.generate(requestFilters()))
          .isInstanceOf(IllegalStateException.class).hasMessage("Service request 77 is not completed");
    }
  }

  @Nested
  class Sections {

    @Test
    @DisplayName("sections(filters) -> request, history, intervention and review sections fully described (AAA)")
    void full() {
      // Arrange
      request(intervention(OWNER_ID, "COMPLETED"));
      doReturn(Optional.of(new String[] {"Freezer 1", "Lima Norte"})).when(assets).fetchEquipmentAndSiteNames(EQUIPMENT_ID);
      doReturn(Optional.of("Luis Rojas")).when(profiles).fetchTechnicianFullName(TECHNICIAN_ID);

      // Act
      var sections = strategy.sections(requestFilters());

      // Assert
      assertThat(sections).extracting(ReportSection::title)
          .containsExactly("Service request", "Status history", "Intervention", "Review");
      var request = sections.get(0);
      assertThat(row(request, "Service request")).isEqualTo("#77");
      assertThat(row(request, "Equipment")).isEqualTo("Freezer 1 (#10)");
      assertThat(row(request, "Site")).isEqualTo("Lima Norte (#3)");
      assertThat(row(request, "Technician")).isEqualTo("Luis Rojas (#6)");
      assertThat(row(request, "Type")).isEqualTo("CORRECTIVE");
      assertThat(row(request, "Created at")).isEqualTo(CREATED.toString());
      assertThat(sections.get(1).rows()).hasSize(1);
      assertThat(sections.get(1).rows().get(0).label()).isEqualTo(COMPLETED.toString());
      assertThat(sections.get(1).rows().get(0).value()).isEqualTo("PENDING to COMPLETED - done");
      var work = sections.get(2);
      assertThat(row(work, "Diagnosis")).isEqualTo("Worn bearing");
      assertThat(row(work, "Spare part 1")).isEqualTo("bearing");
      assertThat(row(work, "Photo 1")).isEqualTo("p1.jpg");
      var review = sections.get(3);
      assertThat(row(review, "Average")).isEqualTo("4.00 / 5");
      assertThat(row(review, "Communication")).isEqualTo("5 / 5");
      assertThat(row(review, "Comment")).isEqualTo("Good");
    }

    @Test
    @DisplayName("sections(filters) with no names, technician, work or review -> readable fallbacks (AAA)")
    void fallbacks() {
      // Arrange
      var history = List.of(new ServiceIntervention.StatusChange(null, "PENDING", CREATED, null));
      request(sparse(null, null, history, null));
      doReturn(Optional.empty()).when(assets).fetchEquipmentAndSiteNames(EQUIPMENT_ID);
      doReturn(Optional.empty()).when(profiles).fetchTechnicianFullName(null);

      // Act
      var sections = strategy.sections(requestFilters());

      // Assert
      var request = sections.get(0);
      assertThat(row(request, "Equipment")).isEqualTo("#10");
      assertThat(row(request, "Site")).isEqualTo("#3");
      assertThat(row(request, "Technician")).isEqualTo("-");
      assertThat(row(request, "Started at")).isEqualTo("-");
      assertThat(sections.get(1).rows().get(0).value()).isEqualTo("Created as PENDING");
      assertThat(row(sections.get(2), "Intervention")).isEqualTo("Not registered");
      assertThat(row(sections.get(3), "Review")).isEqualTo("Not reviewed");
    }

    @Test
    @DisplayName("sections(filters) with a name but a technician profile unresolved -> falls back to the id (AAA)")
    void technicianIdOnly() {
      // Arrange
      var work = new ServiceIntervention.Work("d", "a", List.of(), List.of(), STARTED, COMPLETED);
      var rating = new ServiceIntervention.Rating(1, 2, 3, 2.0, null, COMPLETED);
      request(sparse(work, rating, List.of(), TECHNICIAN_ID));
      doReturn(Optional.of(new String[] {"Freezer 1", "Lima Norte"})).when(assets).fetchEquipmentAndSiteNames(EQUIPMENT_ID);
      doReturn(Optional.empty()).when(profiles).fetchTechnicianFullName(TECHNICIAN_ID);

      // Act
      var sections = strategy.sections(requestFilters());

      // Assert
      assertThat(row(sections.get(0), "Technician")).isEqualTo("#6");
      assertThat(row(sections.get(2), "Spare parts")).isEqualTo("None");
      assertThat(row(sections.get(2), "Photos")).isEqualTo("None");
      assertThat(row(sections.get(3), "Comment")).isEqualTo("-");
    }
  }
}
