package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Resource for the dashboard summary of an owner: the figures its overview cards show, computed by
 * the server in one read.
 */
@Schema(
    name = "DashboardSummaryResponse",
    description = "Totals of an owner's dashboard, computed on the server",
    example = "{\"generated_at\": \"2026-11-09T15:00:00Z\", \"site_count\": 4, "
        + "\"equipment\": {\"total\": 12, \"by_status\": {\"ACTIVE\": 10, \"MAINTENANCE\": 1, \"REPAIR\": 1, "
        + "\"DEACTIVATED\": 0}, \"reporting\": 9, \"silent\": 2, \"never_reported\": 1}, "
        + "\"alerts\": {\"open\": 3, \"acknowledged\": 1, \"active_by_severity\": {\"INFO\": 0, \"WARNING\": 1, "
        + "\"CRITICAL\": 3}}, "
        + "\"service_requests\": {\"active\": 2, \"by_status\": {\"PENDING\": 1, \"ACCEPTED\": 1, "
        + "\"IN_PROGRESS\": 0, \"COMPLETED\": 14, \"CANCELED\": 3}}, "
        + "\"devices\": {\"paired\": 11, \"revoked\": 1}}"
)
public record DashboardSummaryResource(
    @Schema(description = "When the figures were computed", example = "2026-11-09T15:00:00Z")
    Instant generatedAt,

    @Schema(description = "Sites the figures cover: the owner's sites, or 1 when site_id was sent", example = "4")
    int siteCount,

    @Schema(description = "Figures about the units in scope")
    EquipmentFiguresResource equipment,

    @Schema(description = "Figures about the active alerts of those units")
    AlertFiguresResource alerts,

    @Schema(description = "Figures about the owner's service requests in scope")
    ServiceRequestFiguresResource serviceRequests,

    @Schema(description = "Figures about the devices linked to those units")
    DeviceFiguresResource devices
) {

  /**
   * Figures about the units in scope; {@code reporting}, {@code silent} and {@code never_reported}
   * add up to {@code total}.
   */
  @Schema(name = "DashboardEquipmentFigures", description = "Units in scope, by status and by connectivity")
  public record EquipmentFiguresResource(
      @Schema(description = "Units in scope", example = "12")
      long total,

      @Schema(description = "Units per lifecycle status; every status is present, 0 when no unit holds it",
          example = "{\"ACTIVE\": 10, \"MAINTENANCE\": 1, \"REPAIR\": 1, \"DEACTIVATED\": 0}")
      Map<String, Long> byStatus,

      @Schema(description = "Units whose last_reading_at lies within their offline window, 60 seconds times "
          + "missed_sync_windows_for_offline of the applicable policy, judged by the server clock", example = "9")
      long reporting,

      @Schema(description = "Units whose last_reading_at lies outside that window", example = "2")
      long silent,

      @Schema(description = "Units with no reading yet", example = "1")
      long neverReported
  ) {
  }

  /** Figures about the alerts that are OPEN or ACKNOWLEDGED. */
  @Schema(name = "DashboardAlertFigures", description = "Active alerts of the units in scope")
  public record AlertFiguresResource(
      @Schema(description = "Alerts in OPEN", example = "3")
      long open,

      @Schema(description = "Alerts in ACKNOWLEDGED", example = "1")
      long acknowledged,

      @Schema(description = "OPEN and ACKNOWLEDGED alerts per severity; every severity is present",
          example = "{\"INFO\": 0, \"WARNING\": 1, \"CRITICAL\": 3}")
      Map<String, Long> activeBySeverity
  ) {
  }

  /** Figures about the owner's service requests. */
  @Schema(name = "DashboardServiceRequestFigures", description = "Service requests of the owner in scope")
  public record ServiceRequestFiguresResource(
      @Schema(description = "Requests in PENDING, ACCEPTED or IN_PROGRESS", example = "2")
      long active,

      @Schema(description = "Requests per status; every status is present",
          example = "{\"PENDING\": 1, \"ACCEPTED\": 1, \"IN_PROGRESS\": 0, \"COMPLETED\": 14, \"CANCELED\": 3}")
      Map<String, Long> byStatus
  ) {
  }

  /** Figures about the devices linked to the units in scope. */
  @Schema(name = "DashboardDeviceFigures", description = "Devices linked to the units in scope")
  public record DeviceFiguresResource(
      @Schema(description = "Devices PAIRED with a unit in scope", example = "11")
      long paired,

      @Schema(description = "Devices REVOKED and still linked to a unit in scope", example = "1")
      long revoked
  ) {
  }
}
