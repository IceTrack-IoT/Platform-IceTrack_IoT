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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class TemperatureExcursionStrategyTest {

  private static final String TYPE = "TEMPERATURE_EXCURSION";

  @Mock private ExternalAssetsServiceForReporting assets;
  @Mock private ExternalMonitoringServiceForReporting monitoring;
  @InjectMocks private TemperatureExcursionStrategy strategy;

  private void alerts(Long id, long count, double hours) {
    doReturn(count).when(monitoring).countAlertsByEquipmentAndTypeInRange(id, TYPE, FROM, TO);
    doReturn(hours).when(monitoring).fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(id, TYPE, FROM, TO);
  }

  private void samples(Long id, List<double[]> samples) {
    doReturn(samples).when(monitoring).fetchTemperatureSamplesByEquipmentInRange(id, FROM, TO);
  }

  @Test
  @DisplayName("generate(site) -> totals first, then per-unit excursions and thermal KPIs only for units with telemetry (AAA)")
  void mixedUnits() {
    // Arrange: unit 1 has samples and band, unit 2 has none
    doReturn(List.of(1L, 2L)).when(assets).fetchEquipmentIdsBySite(SITE_ID);
    alerts(1L, 2, 1.5);
    alerts(2L, 3, 2.0);
    samples(1L, List.of(new double[] {-18, 0}, new double[] {-5, 600}, new double[] {-18, 1200}));
    samples(2L, List.of());
    doReturn(Optional.of(new double[] {-20, -10})).when(assets).fetchTemperatureThresholdRange(1L);
    doReturn(Optional.of(new double[] {-20, -10})).when(assets).fetchTemperatureThresholdRange(2L);

    // Act
    var kpis = strategy.generate(siteFilters());

    // Assert
    assertThat(kpis).extracting(KpiValue::key).containsExactly(
        "excursion_count", "excursion_hours",
        "excursion_count.unit_1", "excursion_hours.unit_1",
        "mean_kinetic_temperature.unit_1", "temperature_std_dev.unit_1",
        "degree_minutes_out_of_range.unit_1", "time_in_range_pct.unit_1",
        "excursion_count.unit_2", "excursion_hours.unit_2");
    assertThat(kpis.get(0).value()).isEqualTo(5.0);
    assertThat(kpis.get(0).label()).isEqualTo("Total excursions");
    assertThat(kpis.get(1).value()).isEqualTo(3.5);
    assertThat(kpis.get(4).unit()).isEqualTo("C");
    assertThat(kpis.get(5).unit()).isEqualTo("C");
    assertThat(kpis.get(6).unit()).isEqualTo("degree-min");
    assertThat(kpis.get(6).value()).isEqualTo(50.0);
    assertThat(kpis.get(7).unit()).isEqualTo("%");
    assertThat(kpis.get(7).value()).isEqualTo(50.0);
    assertThat(kpis.get(8).value()).isEqualTo(3.0);
    assertThat(kpis.get(9).value()).isEqualTo(2.0);
  }

  @Test
  @DisplayName("generate(equipment filter) -> only that unit, site lookup skipped (AAA)")
  void singleUnit() {
    // Arrange
    alerts(EQUIPMENT_ID, 0, 0.0);
    samples(EQUIPMENT_ID, List.of(new double[] {-18, 0}, new double[] {-18, 60}));
    doReturn(Optional.of(new double[] {-20, -10})).when(assets).fetchTemperatureThresholdRange(EQUIPMENT_ID);

    // Act
    var kpis = strategy.generate(equipmentFilters());

    // Assert
    assertThat(kpis.get(0).key()).isEqualTo("excursion_count");
    assertThat(kpis.get(0).value()).isZero();
    verify(assets, never()).fetchEquipmentIdsBySite(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("generate(units without samples) -> InsufficientTelemetryException (AAA)")
  void noSamples() {
    // Arrange
    alerts(EQUIPMENT_ID, 4, 1.0);
    samples(EQUIPMENT_ID, List.of());
    doReturn(Optional.of(new double[] {-20, -10})).when(assets).fetchTemperatureThresholdRange(EQUIPMENT_ID);

    // Act & Assert
    assertThatThrownBy(() -> strategy.generate(equipmentFilters()))
        .isInstanceOf(InsufficientTelemetryException.class)
        .hasMessage("Insufficient telemetry data for the period");
  }

  @Test
  @DisplayName("generate(unit without threshold band) -> InsufficientTelemetryException (AAA)")
  void noBand() {
    // Arrange
    alerts(EQUIPMENT_ID, 0, 0.0);
    samples(EQUIPMENT_ID, List.of(new double[] {-18, 0}));
    doReturn(Optional.empty()).when(assets).fetchTemperatureThresholdRange(EQUIPMENT_ID);

    // Act & Assert
    assertThatThrownBy(() -> strategy.generate(equipmentFilters())).isInstanceOf(InsufficientTelemetryException.class);
  }

  @Test
  @DisplayName("generate(site without units) -> InsufficientTelemetryException (AAA)")
  void noUnits() {
    // Arrange
    doReturn(List.of()).when(assets).fetchEquipmentIdsBySite(SITE_ID);

    // Act & Assert
    assertThatThrownBy(() -> strategy.generate(siteFilters())).isInstanceOf(InsufficientTelemetryException.class);
  }

  @Test
  @DisplayName("generate(null) -> NullPointerException (AAA)")
  void nullFilters() {
    // Act & Assert
    assertThatThrownBy(() -> strategy.generate(null)).hasMessage("filters must not be null");
  }
}
