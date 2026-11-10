package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * The scope and period of a report.
 *
 * @param siteId       the site to report on, optional
 * @param equipmentId  a single unit to report on, optional
 * @param technicianId a technician to report on, optional
 * @param from         inclusive start of the period
 * @param to           inclusive end of the period
 */
@Schema(name = "ReportFiltersRequest", description = "Scope and period of a report")
public record ReportFiltersResource(
    @Schema(description = "Site to report on; must belong to the caller", example = "1", nullable = true)
    Long siteId,

    @Schema(description = "Single unit to report on; must belong to the caller", nullable = true)
    Long equipmentId,

    @Schema(description = "Technician to report on", nullable = true)
    Long technicianId,

    @NotNull(message = "{validation.not-blank}")
    @Schema(description = "Inclusive start of the period; ISO 8601 with Z or an explicit offset; a value without offset is rejected", example = "2026-10-01T00:00:00Z")
    Instant from,

    @NotNull(message = "{validation.not-blank}")
    @Schema(description = "Inclusive end of the period; ISO 8601 with Z or an explicit offset; a value without offset is rejected", example = "2026-10-31T23:59:59Z")
    Instant to
) {
}
