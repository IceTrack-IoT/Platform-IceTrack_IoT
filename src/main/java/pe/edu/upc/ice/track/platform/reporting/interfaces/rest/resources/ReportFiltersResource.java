package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * The scope and period of a report.
 *
 * <p>{@code service_request_id} is required for a {@code SERVICE_INTERVENTION} report and refused for
 * every other type. {@code from} and {@code to} are required for every other type; a
 * {@code SERVICE_INTERVENTION} report takes the request's own period when they are left out. Both rules
 * depend on the report type, so the command service checks them rather than bean validation.</p>
 *
 * @param siteId           the site to report on, optional
 * @param equipmentId      a single unit to report on, optional
 * @param technicianId     a technician to report on, optional
 * @param serviceRequestId the completed service request a {@code SERVICE_INTERVENTION} report documents
 * @param from             inclusive start of the period
 * @param to               inclusive end of the period
 */
@Schema(name = "ReportFiltersRequest", description = "Scope and period of a report")
public record ReportFiltersResource(
    @Schema(description = "Site to report on; must belong to the caller", example = "1", nullable = true)
    Long siteId,

    @Schema(description = "Single unit to report on; must belong to the caller", nullable = true)
    Long equipmentId,

    @Schema(description = "Technician to report on", nullable = true)
    Long technicianId,

    @Schema(description = "Completed service request of the caller to document; required for SERVICE_INTERVENTION "
        + "and refused for every other type", example = "42", nullable = true)
    Long serviceRequestId,

    @Schema(description = "Inclusive start of the period; ISO 8601 with Z or an explicit offset; a value without "
        + "offset is rejected. Required, except for SERVICE_INTERVENTION, which defaults to the request's own period",
        example = "2026-10-01T00:00:00Z", nullable = true)
    Instant from,

    @Schema(description = "Inclusive end of the period; ISO 8601 with Z or an explicit offset; a value without "
        + "offset is rejected. Required, except for SERVICE_INTERVENTION, which defaults to the request's own period",
        example = "2026-10-31T23:59:59Z", nullable = true)
    Instant to
) {
}
