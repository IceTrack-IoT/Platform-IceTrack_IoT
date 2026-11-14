package pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventResult;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

class EdgeEventTest {

  @Test
  @DisplayName("EdgeEvent(...) -> keeps every supplied value (AAA)")
  void constructorKeepsValues() {
    // Act
    var event = new EdgeEvent(1L, EVENT_UID, DEVICE_ID, EQUIPMENT_ID, EdgeEventType.THERMAL_EXCURSION_DETECTED,
        T0, 8.5, 9.0, EdgeEventResult.ALERT_OPENED, 11L, 12L);

    // Assert
    assertThat(event.getId()).isEqualTo(1L);
    assertThat(event.getEventUid()).isEqualTo(EVENT_UID);
    assertThat(event.getDeviceId()).isEqualTo(DEVICE_ID);
    assertThat(event.getEquipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(event.getType()).isEqualTo(EdgeEventType.THERMAL_EXCURSION_DETECTED);
    assertThat(event.getOccurredAt()).isEqualTo(T0);
    assertThat(event.getTemperatureCelsius()).isEqualTo(8.5);
    assertThat(event.getPeakTemperatureCelsius()).isEqualTo(9.0);
    assertThat(event.getResult()).isEqualTo(EdgeEventResult.ALERT_OPENED);
    assertThat(event.getAlertId()).isEqualTo(11L);
    assertThat(event.getMaintenanceWindowId()).isEqualTo(12L);
  }

  @Test
  @DisplayName("EdgeEvent(...) -> optional references may be null (AAA)")
  void optionalFieldsMayBeNull() {
    var event = new EdgeEvent(null, EVENT_UID, DEVICE_ID, EQUIPMENT_ID, EdgeEventType.MAINTENANCE_WINDOW_STARTED,
        T0, null, null, EdgeEventResult.MAINTENANCE_WINDOW_OPENED, null, null);

    assertThat(event.getId()).isNull();
    assertThat(event.getTemperatureCelsius()).isNull();
    assertThat(event.getAlertId()).isNull();
    assertThat(event.getMaintenanceWindowId()).isNull();
  }

  @Test
  @DisplayName("EdgeEvent(...) -> requires uid, device, equipment, type, instant and result (AAA)")
  void constructorRequiresMandatoryFields() {
    var type = EdgeEventType.THERMAL_EXCURSION_DETECTED;
    var result = EdgeEventResult.ALERT_OPENED;
    assertThrows(NullPointerException.class,
        () -> new EdgeEvent(null, null, DEVICE_ID, EQUIPMENT_ID, type, T0, null, null, result, null, null));
    assertThrows(NullPointerException.class,
        () -> new EdgeEvent(null, UUID.randomUUID(), null, EQUIPMENT_ID, type, T0, null, null, result, null, null));
    assertThrows(NullPointerException.class,
        () -> new EdgeEvent(null, UUID.randomUUID(), DEVICE_ID, null, type, T0, null, null, result, null, null));
    assertThrows(NullPointerException.class,
        () -> new EdgeEvent(null, UUID.randomUUID(), DEVICE_ID, EQUIPMENT_ID, null, T0, null, null, result, null, null));
    assertThrows(NullPointerException.class,
        () -> new EdgeEvent(null, UUID.randomUUID(), DEVICE_ID, EQUIPMENT_ID, type, null, null, null, result, null, null));
    assertThrows(NullPointerException.class,
        () -> new EdgeEvent(null, UUID.randomUUID(), DEVICE_ID, EQUIPMENT_ID, type, T0, null, null, null, null, null));
  }

  @Test
  @DisplayName("edgeEvent fixture -> builds a valid event (AAA)")
  void fixtureIsValid() {
    assertThat(edgeEvent(EdgeEventType.THERMAL_EXCURSION_NORMALIZED, EdgeEventResult.ALERT_RESOLVED).getResult())
        .isEqualTo(EdgeEventResult.ALERT_RESOLVED);
  }
}
