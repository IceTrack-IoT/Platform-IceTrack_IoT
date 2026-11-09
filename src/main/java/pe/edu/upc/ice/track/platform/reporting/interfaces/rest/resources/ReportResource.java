package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Resource representing a report, as returned to API consumers.
 *
 * <p>{@code download_url} is present only for a {@code COMPLETED} report that was read by id; it is
 * omitted otherwise, and a new one is issued on every read.</p>
 *
 * @param id            identifier of the report
 * @param name          display name of the report
 * @param type          the report type's enum name
 * @param format        the output format's enum name
 * @param status        {@code PENDING}, {@code GENERATING}, {@code COMPLETED} or {@code FAILED}
 * @param failureReason why the report failed; present only when {@code FAILED}
 * @param resultPayload a JSON array of the KPIs once {@code COMPLETED}; {@code null} otherwise
 * @param userId        identifier of the user who requested the report
 * @param siteId        the site the report covers, nullable
 * @param equipmentId   the unit the report covers, nullable
 * @param technicianId  the technician the report covers, nullable
 * @param from          inclusive start of the period
 * @param to            inclusive end of the period
 * @param requestedAt   when the report was requested
 * @param completedAt   when the report reached {@code COMPLETED} or {@code FAILED}; {@code null} while in progress
 * @param downloadUrl   a signed URL valid for 15 minutes; present only for a {@code COMPLETED} report read by id
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "ReportResponse",
    description = "A report, with its generation status",
    example = "{\"id\": 8, \"name\": \"Excursions - October\", \"type\": \"TEMPERATURE_EXCURSION\", "
        + "\"format\": \"PDF\", \"status\": \"COMPLETED\", \"user_id\": 42, \"site_id\": 1, "
        + "\"from\": \"2026-10-01T00:00:00Z\", \"to\": \"2026-10-31T23:59:59Z\", "
        + "\"requested_at\": \"2026-11-01T08:00:00Z\", \"completed_at\": \"2026-11-01T08:00:09Z\", "
        + "\"download_url\": \"https://storage.googleapis.com/icetrack-reports/reports/7/8.pdf?X-Goog-Signature=abc\"}")
public record ReportResource(
    @Schema(description = "Report unique identifier", example = "8")
    Long id,

    @Schema(description = "Display name of the report", example = "Excursions - October")
    String name,

    @Schema(description = "Report type's enum name", example = "TEMPERATURE_EXCURSION",
        allowableValues = {"MAINTENANCE_COMPLIANCE", "EQUIPMENT_UPTIME", "TECHNICIAN_PERFORMANCE",
            "TEMPERATURE_EXCURSION"})
    String type,

    @Schema(description = "Output format's enum name", example = "PDF", allowableValues = {"PDF", "XLSX", "CSV"})
    String format,

    @Schema(description = "Current generation status", example = "COMPLETED",
        allowableValues = {"PENDING", "GENERATING", "COMPLETED", "FAILED"})
    String status,

    @Schema(description = "Why the report failed; present only when FAILED", nullable = true,
        example = "Insufficient telemetry data for the period")
    String failureReason,

    @Schema(description = "JSON array of the KPIs once completed; null otherwise", nullable = true,
        example = "[{\"label\": \"Total excursions\", \"value\": 2.0, \"unit\": \"count\"}]")
    String resultPayload,

    @Schema(description = "Identifier of the user who requested the report", example = "42")
    Long userId,

    @Schema(description = "Site the report covers", example = "1", nullable = true)
    Long siteId,

    @Schema(description = "Unit the report covers", nullable = true)
    Long equipmentId,

    @Schema(description = "Technician the report covers", nullable = true)
    Long technicianId,

    @Schema(description = "Inclusive start of the period", example = "2026-10-01T00:00:00Z")
    Instant from,

    @Schema(description = "Inclusive end of the period", example = "2026-10-31T23:59:59Z")
    Instant to,

    @Schema(description = "When the report was requested", example = "2026-11-01T08:00:00Z")
    Instant requestedAt,

    @Schema(description = "When the report reached COMPLETED or FAILED; null while in progress", nullable = true,
        example = "2026-11-01T08:00:09Z")
    Instant completedAt,

    @Schema(description = "Signed download URL valid for 15 minutes; only for a COMPLETED report read by id",
        nullable = true)
    String downloadUrl
) {
}
