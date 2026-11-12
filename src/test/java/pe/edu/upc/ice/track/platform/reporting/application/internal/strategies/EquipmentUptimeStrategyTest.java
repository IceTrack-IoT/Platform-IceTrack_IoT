package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalAssetsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalMonitoringServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class EquipmentUptimeStrategyTest {

  @Mock private ExternalAssetsServiceForReporting assets;
  @Mock private ExternalMonitoringServiceForReporting monitoring;
  @InjectMocks private EquipmentUptimeStrategy strategy;

  private void offline(Long equipmentId, double hours) {
    doReturn(hours).when(monitoring)
        .fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(equipmentId, "DEVICE_OFFLINE", FROM, TO);
  }

  @Test
  @DisplayName("generate(site filter) -> average first, then one uptime KPI per unit of the site (AAA)")
  void siteUnits() {
    // Arrange: 24 h window; unit 1 offline 6 h (75 %), unit 2 never offline (100 %)
    doReturn(List.of(1L, 2L)).when(assets).fetchEquipmentIdsBySite(SITE_ID);
    offline(1L, 6.0);
    offline(2L, 0.0);

    // Act
    var kpis = strategy.generate(siteFilters());

    // Assert
    assertThat(kpis).extracting(KpiValue::key).containsExactly("average_uptime_pct", "uptime_pct.unit_1", "uptime_pct.unit_2");
    assertThat(kpis.get(0).value()).isCloseTo(87.5, within(1e-9));
    assertThat(kpis.get(1).value()).isCloseTo(75.0, within(1e-9));
    assertThat(kpis.get(1).label()).isEqualTo("Uptime - Unit 1");
    assertThat(kpis.get(2).value()).isCloseTo(100.0, within(1e-9));
    assertThat(kpis).extracting(KpiValue::unit).containsOnly("%");
  }

  @Test
  @DisplayName("generate(equipment filter) -> only that unit, site lookup skipped (AAA)")
  void singleUnit() {
    // Arrange
    offline(EQUIPMENT_ID, 12.0);

    // Act
    var kpis = strategy.generate(equipmentFilters());

    // Assert
    assertThat(kpis).extracting(KpiValue::key).containsExactly("average_uptime_pct", "uptime_pct.unit_10");
    assertThat(kpis.get(0).value()).isCloseTo(50.0, within(1e-9));
    verify(assets, never()).fetchEquipmentIdsBySite(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("generate(offline longer than the window) -> uptime floored at 0 % (AAA)")
  void floorAtZero() {
    // Arrange
    offline(EQUIPMENT_ID, 99.0);

    // Act
    var kpis = strategy.generate(equipmentFilters());

    // Assert
    assertThat(kpis.get(1).value()).isZero();
  }

  @Test
  @DisplayName("generate(zero-length window) -> 0 % instead of dividing by zero (AAA)")
  void zeroWindow() {
    // Arrange
    var filters = new pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters(null,
        new pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId(EQUIPMENT_ID), null,
        new pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.DateRange(FROM, FROM));
    doReturn(0.0).when(monitoring).fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(EQUIPMENT_ID, "DEVICE_OFFLINE", FROM, FROM);

    // Act
    var kpis = strategy.generate(filters);

    // Assert
    assertThat(kpis.get(1).value()).isZero();
  }

  @Test
  @DisplayName("generate(site without units) -> single average of 0 % (AAA)")
  void noUnits() {
    // Arrange
    doReturn(List.of()).when(assets).fetchEquipmentIdsBySite(SITE_ID);

    // Act
    var kpis = strategy.generate(siteFilters());

    // Assert
    assertThat(kpis).hasSize(1);
    assertThat(kpis.get(0).key()).isEqualTo("average_uptime_pct");
    assertThat(kpis.get(0).value()).isZero();
  }

  @Test
  @DisplayName("generate(null) -> NullPointerException (AAA)")
  void nullFilters() {
    // Act & Assert
    assertThatThrownBy(() -> strategy.generate(null)).isInstanceOf(NullPointerException.class).hasMessage("filters must not be null");
  }

  @Test
  @DisplayName("sections(filters) -> default: no extra sections (AAA)")
  void noSections() {
    // Act & Assert
    assertThat(strategy.sections(siteFilters())).isEmpty();
  }
}
