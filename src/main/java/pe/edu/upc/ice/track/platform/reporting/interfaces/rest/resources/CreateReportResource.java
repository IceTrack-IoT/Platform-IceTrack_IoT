package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Resource received to request a new report.
 *
 * <p>No owner or requester field: the requester is the authenticated caller.</p>
 *
 * @param name    display name of the report
 * @param type    the report type, by its enum name
 * @param format  the output format: {@code PDF}, {@code XLSX} or {@code CSV}
 * @param filters what to include and over which period
 */
@Schema(
    name = "CreateReportRequest",
    description = "Request to generate a report; the report is generated in the background",
    example = "{\"name\": \"Excursions - October\", \"type\": \"TEMPERATURE_EXCURSION\", \"format\": \"PDF\", "
        + "\"filters\": {\"site_id\": 1, \"equipment_id\": null, \"technician_id\": null, "
        + "\"from\": \"2026-10-01T00:00:00\", \"to\": \"2026-10-31T23:59:59\"}}")
public record CreateReportResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 150, message = "{validation.size.max}")
    @Schema(description = "Display name of the report", example = "Excursions - October", maxLength = 150)
    String name,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Report type, by its enum name", example = "TEMPERATURE_EXCURSION",
        allowableValues = {"MAINTENANCE_COMPLIANCE", "EQUIPMENT_UPTIME", "TECHNICIAN_PERFORMANCE",
            "TEMPERATURE_EXCURSION"})
    String type,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Output format", example = "PDF", allowableValues = {"PDF", "XLSX", "CSV"})
    String format,

    @Valid
    @NotNull(message = "{validation.not-blank}")
    @Schema(description = "Scope and period of the report")
    ReportFiltersResource filters
) {
}
