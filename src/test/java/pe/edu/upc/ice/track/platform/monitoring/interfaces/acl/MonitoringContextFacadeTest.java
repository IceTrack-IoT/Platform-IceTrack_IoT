package pe.edu.upc.ice.track.platform.monitoring.interfaces.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.MaintenanceWindow;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.MaintenanceWindowRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class MonitoringContextFacadeTest {

  private static final Instant FROM = T0;
  private static final Instant TO = T0.plus(Duration.ofHours(10));

  @Mock
  private AlertRepository alertRepository;
  @Mock
  private AlertPolicyRepository alertPolicyRepository;
  @Mock
  private MaintenanceWindowRepository maintenanceWindowRepository;
  @Mock
  private SensorReadingRepository sensorReadingRepository;

  @InjectMocks
  private MonitoringContextFacade facade;

  private static Instant hours(double offset) {
    return T0.plusSeconds((long) (offset * 3600));
  }

  /** An alert whose opening and resolution instants are controlled by the test. */
  private static Alert alert(AlertStatus status, Instant openedAt, Instant resolvedAt) {
    var alert = persistedTemperatureAlert();
    alert.restoreState(status, openedAt, null, null, null, resolvedAt, null, null, null, null);
    return alert;
  }

  private static MaintenanceWindow window(Instant startedAt, Instant endedAt) {
    return new MaintenanceWindow(1L, START_EVENT_UID, EQUIPMENT_ID, MaintenanceOrigin.BUTTON, startedAt,
        startedAt.plus(Duration.ofMinutes(30)), endedAt, endedAt == null ? null : ClosureReason.MANUAL_BUTTON,
        null, null);
  }

  private void givenWindows(MaintenanceWindow... windows) {
    when(maintenanceWindowRepository.findByEquipmentIdAndStartedAtBetween(
        EQUIPMENT_ID, FROM.minus(Duration.ofDays(7)), TO)).thenReturn(Arrays.asList(windows));
  }

  @Test
  @DisplayName("fetchApplicableAlertPolicy(equipmentId) -> the applicable policy's settings (AAA)")
  void applicablePolicyForEquipment() {
    // Arrange
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.of(equipmentPolicy()));

    // Act
    var settings = facade.fetchApplicableAlertPolicy(EQUIPMENT_ID);

    // Assert
    assertThat(settings).isEqualTo(new AlertPolicySettings(5, 1.5, 4));
  }

  @Test
  @DisplayName("fetchApplicableAlertPolicy(null) -> the platform default policy stored (AAA)")
  void applicablePolicyWithoutEquipmentUsesStoredDefault() {
    // Arrange
    when(alertPolicyRepository.findDefaultPolicy()).thenReturn(Optional.of(equipmentPolicy()));

    // Act
    var settings = facade.fetchApplicableAlertPolicy(null);

    // Assert
    assertThat(settings).isEqualTo(new AlertPolicySettings(5, 1.5, 4));
    verify(alertPolicyRepository, never()).findApplicableTo(any());
  }

  @Test
  @DisplayName("fetchApplicableAlertPolicy(equipmentId) with nothing stored -> built-in defaults (AAA)")
  void applicablePolicyFallsBackToBuiltInDefaults() {
    // Arrange
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var settings = facade.fetchApplicableAlertPolicy(EQUIPMENT_ID);

    // Assert
    assertThat(settings).isEqualTo(new AlertPolicySettings(
        AlertPolicy.DEFAULT_SUSTAINED_EXCURSION_MINUTES,
        AlertPolicy.DEFAULT_HYSTERESIS_MARGIN_CELSIUS,
        AlertPolicy.DEFAULT_MISSED_SYNC_WINDOWS_FOR_OFFLINE));
  }

  @Test
  @DisplayName("fetchOfflineSilenceSeconds(null or empty or only nulls) -> empty map, no reads (AAA)")
  void offlineSilenceWithoutIds() {
    // Arrange & Act & Assert
    assertThat(facade.fetchOfflineSilenceSeconds(null)).isEmpty();
    assertThat(facade.fetchOfflineSilenceSeconds(List.of())).isEmpty();
    assertThat(facade.fetchOfflineSilenceSeconds(Arrays.asList((Long) null))).isEmpty();
    verifyNoInteractions(alertPolicyRepository);
  }

  @Test
  @DisplayName("fetchOfflineSilenceSeconds(ids) -> own policy where stored, default for the rest (AAA)")
  void offlineSilencePerEquipment() {
    // Arrange: own policy tolerates 4 windows (240 s); the built-in default 3 windows (180 s)
    when(alertPolicyRepository.findByEquipmentIdIn(Set.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID)))
        .thenReturn(List.of(persistedEquipmentPolicy()));
    when(alertPolicyRepository.findDefaultPolicy()).thenReturn(Optional.empty());

    // Act
    var silence = facade.fetchOfflineSilenceSeconds(Arrays.asList(EQUIPMENT_ID, OTHER_EQUIPMENT_ID, null));

    // Assert
    assertThat(silence).containsEntry(EQUIPMENT_ID, 240L).containsEntry(OTHER_EQUIPMENT_ID, 180L).hasSize(2);
  }

  @Test
  @DisplayName("fetchOfflineSilenceSeconds(ids all with own policy) -> never reads the default policy (AAA)")
  void offlineSilenceSkipsDefaultWhenEveryUnitHasPolicy() {
    // Arrange
    when(alertPolicyRepository.findByEquipmentIdIn(Set.of(EQUIPMENT_ID)))
        .thenReturn(List.of(persistedEquipmentPolicy()));

    // Act
    var silence = facade.fetchOfflineSilenceSeconds(List.of(EQUIPMENT_ID));

    // Assert
    assertThat(silence).containsOnly(java.util.Map.entry(EQUIPMENT_ID, 240L));
    verify(alertPolicyRepository, never()).findDefaultPolicy();
  }

  @Test
  @DisplayName("fetchAlertEquipmentId(alertId) -> the alert's equipment, or 0 when missing (AAA)")
  void alertEquipmentId() {
    // Arrange
    when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(persistedTemperatureAlert()));
    org.mockito.Mockito.doReturn(Optional.empty()).when(alertRepository).findById(404L);

    // Act & Assert
    assertThat(facade.fetchAlertEquipmentId(ALERT_ID)).isEqualTo(EQUIPMENT_ID);
    assertThat(facade.fetchAlertEquipmentId(404L)).isZero();
    assertThat(facade.fetchAlertEquipmentId(null)).isZero();
  }

  @Test
  @DisplayName("countOpenAlertsByEquipment(equipmentId) -> number of OPEN and ACKNOWLEDGED alerts (AAA)")
  void countOpenAlerts() {
    // Arrange
    when(alertRepository.findByEquipmentIdAndStatusIn(EQUIPMENT_ID, List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)))
        .thenReturn(List.of(persistedTemperatureAlert(), persistedTemperatureAlert()));

    // Act & Assert
    assertThat(facade.countOpenAlertsByEquipment(EQUIPMENT_ID)).isEqualTo(2);
    assertThat(facade.countOpenAlertsByEquipment(null)).isZero();
  }

  @Test
  @DisplayName("fetchLatestAlertIdByEquipmentAndStatus(...) -> id of the first alert in that status, else 0 (AAA)")
  void latestAlertIdByStatus() {
    // Arrange
    var open = persistedTemperatureAlert();
    when(alertRepository.findByEquipmentIdAndStatusIn(EQUIPMENT_ID, List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)))
        .thenReturn(List.of(open));

    // Act & Assert
    assertThat(facade.fetchLatestAlertIdByEquipmentAndStatus(EQUIPMENT_ID, "open")).isEqualTo(ALERT_ID);
    assertThat(facade.fetchLatestAlertIdByEquipmentAndStatus(EQUIPMENT_ID, "ACKNOWLEDGED")).isZero();
    assertThat(facade.fetchLatestAlertIdByEquipmentAndStatus(EQUIPMENT_ID, "bogus")).isZero();
    assertThat(facade.fetchLatestAlertIdByEquipmentAndStatus(null, "OPEN")).isZero();
    assertThat(facade.fetchLatestAlertIdByEquipmentAndStatus(EQUIPMENT_ID, null)).isZero();
  }

  @Test
  @DisplayName("fetchLatestAlertIdByEquipmentAndStatus(...) unsaved alert -> 0 (AAA)")
  void latestAlertIdOfUnsavedAlert() {
    // Arrange
    when(alertRepository.findByEquipmentIdAndStatusIn(EQUIPMENT_ID, List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)))
        .thenReturn(List.of(temperatureAlert()));

    // Act & Assert
    assertThat(facade.fetchLatestAlertIdByEquipmentAndStatus(EQUIPMENT_ID, "OPEN")).isZero();
  }

  @Test
  @DisplayName("countAlertsByEquipmentAndTypeInRange(...) -> excludes dismissed alerts and those opened in maintenance (AAA)")
  void countsAlertsOutsideMaintenance() {
    // Arrange: windows [-1h,+1h] (clipped to [0,1h]), [2h,3h] and an active one from 8h
    givenWindows(window(hours(-1), hours(1)), window(hours(2), hours(3)), window(hours(8), null));
    when(alertRepository.findByEquipmentIdAndTypeOpenedBetween(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION, FROM, TO))
        .thenReturn(List.of(
            alert(AlertStatus.OPEN, hours(0.5), null),
            alert(AlertStatus.OPEN, hours(1.5), null),
            alert(AlertStatus.OPEN, hours(2.5), null),
            alert(AlertStatus.RESOLVED, hours(3), hours(4)),
            alert(AlertStatus.DISMISSED, hours(5), null),
            alert(AlertStatus.OPEN, hours(9), null)));

    // Act
    var count = facade.countAlertsByEquipmentAndTypeInRange(EQUIPMENT_ID, "temperature_excursion", FROM, TO);

    // Assert: only the 1.5h alert and the one opened exactly when the second window closed
    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("countAlertsByEquipmentAndTypeInRange(unknown type) -> IllegalArgumentException (AAA)")
  void countRejectsUnknownType() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> facade.countAlertsByEquipmentAndTypeInRange(EQUIPMENT_ID, "NOPE", FROM, TO))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("countAlertsByEquipmentAndTypeInRange(null equipment) -> no maintenance windows are read (AAA)")
  void countWithoutEquipmentReadsNoWindows() {
    // Arrange
    when(alertRepository.findByEquipmentIdAndTypeOpenedBetween(null, AlertType.DEVICE_OFFLINE, FROM, TO))
        .thenReturn(List.of());

    // Act
    var count = facade.countAlertsByEquipmentAndTypeInRange(null, "DEVICE_OFFLINE", FROM, TO);

    // Assert
    assertThat(count).isZero();
    verifyNoInteractions(maintenanceWindowRepository);
  }

  @Test
  @DisplayName("fetchTotalAlertDurationHours...(...) -> clips to the range and subtracts merged maintenance time (AAA)")
  void totalDurationSubtractsMergedMaintenance() {
    // Arrange: overlapping windows [2h,3h] and [2.5h,4h] merge into [2h,4h]
    givenWindows(window(hours(2), hours(3)), window(hours(2.5), hours(4)));
    when(alertRepository.findByEquipmentIdAndTypeOpenedBetween(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION, FROM, TO))
        .thenReturn(List.of(
            alert(AlertStatus.RESOLVED, hours(1), hours(5)),     // 4h - 2h = 2h
            alert(AlertStatus.OPEN, hours(-1), null),            // clipped to [0,10h] = 10h - 2h = 8h
            alert(AlertStatus.DISMISSED, hours(1), hours(9)),    // ignored
            alert(AlertStatus.RESOLVED, hours(6), hours(6)),     // zero length
            alert(AlertStatus.RESOLVED, hours(7), hours(20))));  // clipped to [7h,10h] = 3h

    // Act
    var hoursTotal = facade.fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(
        EQUIPMENT_ID, "TEMPERATURE_EXCURSION", FROM, TO);

    // Assert
    assertThat(hoursTotal).isEqualTo(13.0);
  }

  @Test
  @DisplayName("fetchTotalAlertDurationHours...(alert entirely inside maintenance) -> never negative (AAA)")
  void totalDurationNeverNegative() {
    // Arrange
    givenWindows(window(hours(1), hours(5)));
    when(alertRepository.findByEquipmentIdAndTypeOpenedBetween(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION, FROM, TO))
        .thenReturn(List.of(alert(AlertStatus.RESOLVED, hours(2), hours(3))));

    // Act
    var hoursTotal = facade.fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(
        EQUIPMENT_ID, "TEMPERATURE_EXCURSION", FROM, TO);

    // Assert
    assertThat(hoursTotal).isZero();
  }

  @Test
  @DisplayName("fetchTemperatureSamplesByEquipmentInRange(...) -> {avg, epoch} pairs sorted, without maintenance readings (AAA)")
  void temperatureSamples() {
    // Arrange
    var later = sensorReading(hours(2), 4.0, 6.0, 8.0, 10, false);
    var earlier = sensorReading(hours(1), 3.0, 5.0, 7.0, 10, false);
    var duringMaintenance = sensorReading(hours(1.5), 3.0, 9.0, 11.0, 10, true);
    when(sensorReadingRepository.findByEquipmentIdAndRecordedAtBetween(EQUIPMENT_ID, FROM, TO))
        .thenReturn(List.of(later, duringMaintenance, earlier));

    // Act
    var samples = facade.fetchTemperatureSamplesByEquipmentInRange(EQUIPMENT_ID, FROM, TO);

    // Assert
    assertThat(samples).hasSize(2);
    assertThat(samples.get(0)).containsExactly(5.0, (double) hours(1).getEpochSecond());
    assertThat(samples.get(1)).containsExactly(6.0, (double) hours(2).getEpochSecond());
  }

  @Test
  @DisplayName("fetchTemperatureSamplesByEquipmentInRange(null) -> empty list without reading (AAA)")
  void temperatureSamplesWithoutEquipment() {
    // Arrange & Act & Assert
    assertThat(facade.fetchTemperatureSamplesByEquipmentInRange(null, FROM, TO)).isEmpty();
    verifyNoInteractions(sensorReadingRepository);
  }
}
