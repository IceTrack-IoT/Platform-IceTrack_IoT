package pe.edu.upc.ice.track.platform.monitoring.domain.model.readmodels;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.T0;

class DashboardSummaryTest {

  private static DashboardSummary.EquipmentFigures equipment() {
    return new DashboardSummary.EquipmentFigures(3, Map.of("ACTIVE", 3L), 2, 1, 0);
  }

  private static DashboardSummary.AlertFigures alerts() {
    return new DashboardSummary.AlertFigures(1, 2, Map.of(AlertSeverity.CRITICAL, 3L));
  }

  private static DashboardSummary.ServiceRequestFigures requests() {
    return new DashboardSummary.ServiceRequestFigures(4, Map.of("PENDING", 4L));
  }

  private static DashboardSummary.DeviceFigures devices() {
    return new DashboardSummary.DeviceFigures(5, 1);
  }

  @Test
  @DisplayName("DashboardSummary(...) -> keeps every section (AAA)")
  void keepsSections() {
    // Act
    var summary = new DashboardSummary(T0, 2, equipment(), alerts(), requests(), devices());

    // Assert
    assertThat(summary.generatedAt()).isEqualTo(T0);
    assertThat(summary.siteCount()).isEqualTo(2);
    assertThat(summary.equipment().total()).isEqualTo(3);
    assertThat(summary.alerts().open()).isEqualTo(1);
    assertThat(summary.serviceRequests().active()).isEqualTo(4);
    assertThat(summary.devices().paired()).isEqualTo(5);
  }

  @Test
  @DisplayName("DashboardSummary(...) -> requires every section (AAA)")
  void requiresSections() {
    assertThrows(NullPointerException.class, () -> new DashboardSummary(null, 0, equipment(), alerts(), requests(), devices()));
    assertThrows(NullPointerException.class, () -> new DashboardSummary(T0, 0, null, alerts(), requests(), devices()));
    assertThrows(NullPointerException.class, () -> new DashboardSummary(T0, 0, equipment(), null, requests(), devices()));
    assertThrows(NullPointerException.class, () -> new DashboardSummary(T0, 0, equipment(), alerts(), null, devices()));
    assertThrows(NullPointerException.class, () -> new DashboardSummary(T0, 0, equipment(), alerts(), requests(), null));
  }

  @Test
  @DisplayName("EquipmentFigures(...) -> copies its map defensively, preserving order, and exposes it read-only (AAA)")
  void equipmentMapIsDefensive() {
    // Arrange
    var source = new LinkedHashMap<String, Long>();
    source.put("B", 1L);
    source.put("A", 2L);

    // Act
    var figures = new DashboardSummary.EquipmentFigures(3, source, 0, 0, 0);
    source.put("C", 3L);

    // Assert
    assertThat(figures.byStatus()).containsExactly(Map.entry("B", 1L), Map.entry("A", 2L));
    assertThrows(UnsupportedOperationException.class, () -> figures.byStatus().put("X", 1L));
  }

  @Test
  @DisplayName("AlertFigures(...) and ServiceRequestFigures(...) -> copy their maps defensively (AAA)")
  void otherMapsAreDefensive() {
    var severities = new HashMap<AlertSeverity, Long>();
    severities.put(AlertSeverity.INFO, 1L);
    var statuses = new HashMap<String, Long>();
    statuses.put("PENDING", 1L);

    var alertFigures = new DashboardSummary.AlertFigures(0, 0, severities);
    var requestFigures = new DashboardSummary.ServiceRequestFigures(0, statuses);
    severities.clear();
    statuses.clear();

    assertThat(alertFigures.activeBySeverity()).containsEntry(AlertSeverity.INFO, 1L);
    assertThat(requestFigures.byStatus()).containsEntry("PENDING", 1L);
    assertThrows(UnsupportedOperationException.class, () -> alertFigures.activeBySeverity().clear());
    assertThrows(UnsupportedOperationException.class, () -> requestFigures.byStatus().clear());
  }
}
