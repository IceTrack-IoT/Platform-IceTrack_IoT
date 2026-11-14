package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalAssetsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalServiceRequestsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.doReturn;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

class MaintenanceComplianceAndTechnicianStrategyTest {

  @Nested
  @ExtendWith(MockitoExtension.class)
  class Compliance {

    @Mock private ExternalAssetsServiceForReporting assets;
    @Mock private ExternalServiceRequestsServiceForReporting serviceRequests;
    @InjectMocks private MaintenanceComplianceStrategy strategy;

    @Test
    @DisplayName("generate(site) -> units in scope, compliant units and compliance rate (AAA)")
    void rate() {
      // Arrange: 4 units, 3 had preventive maintenance but id 99 is not in the site
      doReturn(List.of(1L, 2L, 3L, 4L)).when(assets).fetchEquipmentIdsBySite(SITE_ID);
      doReturn(List.of(1L, 2L, 99L)).when(serviceRequests)
          .fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(SITE_ID, FROM, TO);

      // Act
      var kpis = strategy.generate(siteFilters());

      // Assert
      assertThat(kpis).extracting(KpiValue::key).containsExactly("units_in_scope", "compliant_units", "compliance_rate_pct");
      assertThat(kpis.get(0).value()).isEqualTo(4.0);
      assertThat(kpis.get(1).value()).isEqualTo(2.0);
      assertThat(kpis.get(2).value()).isCloseTo(50.0, within(1e-9));
      assertThat(kpis).extracting(KpiValue::unit).containsExactly("count", "count", "%");
    }

    @Test
    @DisplayName("generate(site without units) -> all zeros, no division by zero (AAA)")
    void noUnits() {
      // Arrange
      doReturn(List.of()).when(assets).fetchEquipmentIdsBySite(SITE_ID);
      doReturn(List.of()).when(serviceRequests).fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(SITE_ID, FROM, TO);

      // Act
      var kpis = strategy.generate(siteFilters());

      // Assert
      assertThat(kpis).extracting(KpiValue::value).containsExactly(0.0, 0.0, 0.0);
    }

    @Test
    @DisplayName("generate(null / filters without site) -> NullPointerException (AAA)")
    void invalid() {
      // Act & Assert
      assertThatThrownBy(() -> strategy.generate(null)).hasMessage("filters must not be null");
      assertThatThrownBy(() -> strategy.generate(technicianFilters()))
          .isInstanceOf(NullPointerException.class).hasMessage("siteId must not be null for a compliance report");
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class TechnicianPerformance {

    @Mock private ExternalServiceRequestsServiceForReporting serviceRequests;
    @InjectMocks private TechnicianPerformanceStrategy strategy;

    @Test
    @DisplayName("generate(technician with reviews) -> completed count and average rating (AAA)")
    void withRating() {
      // Arrange
      doReturn(7L).when(serviceRequests).countCompletedServiceRequestsByTechnicianInRange(TECHNICIAN_ID, FROM, TO);
      doReturn(Optional.of(4.25)).when(serviceRequests).fetchAverageRatingByTechnician(TECHNICIAN_ID);

      // Act
      var kpis = strategy.generate(technicianFilters());

      // Assert
      assertThat(kpis).extracting(KpiValue::key).containsExactly("completed_service_requests", "average_rating");
      assertThat(kpis.get(0).value()).isEqualTo(7.0);
      assertThat(kpis.get(0).unit()).isEqualTo("count");
      assertThat(kpis.get(1).value()).isEqualTo(4.25);
      assertThat(kpis.get(1).unit()).isEqualTo("stars");
    }

    @Test
    @DisplayName("generate(technician never reviewed) -> rating KPI omitted rather than zero (AAA)")
    void withoutRating() {
      // Arrange
      doReturn(0L).when(serviceRequests).countCompletedServiceRequestsByTechnicianInRange(TECHNICIAN_ID, FROM, TO);
      doReturn(Optional.empty()).when(serviceRequests).fetchAverageRatingByTechnician(TECHNICIAN_ID);

      // Act
      var kpis = strategy.generate(technicianFilters());

      // Assert
      assertThat(kpis).extracting(KpiValue::key).containsExactly("completed_service_requests");
      assertThat(kpis.get(0).value()).isZero();
    }

    @Test
    @DisplayName("generate(null / filters without technician) -> NullPointerException (AAA)")
    void invalid() {
      // Act & Assert
      assertThatThrownBy(() -> strategy.generate(null)).hasMessage("filters must not be null");
      assertThatThrownBy(() -> strategy.generate(siteFilters()))
          .isInstanceOf(NullPointerException.class).hasMessage("technicianId must not be null for a performance report");
    }
  }
}
