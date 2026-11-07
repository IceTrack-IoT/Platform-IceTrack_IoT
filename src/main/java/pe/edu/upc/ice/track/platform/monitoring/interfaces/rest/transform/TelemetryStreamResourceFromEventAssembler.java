package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.AlertStatusChangedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.ReadingRecordedEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertStatusChangedStreamResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.ReadingRecordedStreamResource;

/**
 * Assembler converting the monitoring domain events into the data of {@code telemetry-update}
 * stream events.
 */
public final class TelemetryStreamResourceFromEventAssembler {

  /** {@code type} of the data announcing a reading batch. */
  public static final String READING_RECORDED = "READING_RECORDED";

  /** {@code type} of the data announcing an alert status change. */
  public static final String ALERT_STATUS_CHANGED = "ALERT_STATUS_CHANGED";

  private TelemetryStreamResourceFromEventAssembler() {
  }

  /**
   * @param event the reading batch just recorded
   * @return the stream event data
   */
  public static ReadingRecordedStreamResource toResourceFromEvent(ReadingRecordedEvent event) {
    return new ReadingRecordedStreamResource(
        READING_RECORDED,
        event.readingUid() == null ? null : event.readingUid().toString(),
        event.equipmentId(),
        event.deviceId(),
        event.minTemperature(),
        event.maxTemperature(),
        event.avgTemperature(),
        event.humidity(),
        event.sampleCount(),
        event.recordedAt(),
        event.receivedAt(),
        event.underMaintenance());
  }

  /**
   * @param event the alert status change
   * @return the stream event data
   */
  public static AlertStatusChangedStreamResource toResourceFromEvent(AlertStatusChangedEvent event) {
    return new AlertStatusChangedStreamResource(
        ALERT_STATUS_CHANGED,
        event.alertId(),
        event.equipmentId(),
        event.type() == null ? null : event.type().name(),
        event.severity() == null ? null : event.severity().name(),
        event.previousStatus() == null ? null : event.previousStatus().name(),
        event.status() == null ? null : event.status().name(),
        event.peakTemperature(),
        event.changedAt());
  }
}
