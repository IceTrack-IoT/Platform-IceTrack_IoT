package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Everything a client shell needs right after sign-in, in one answer.
 *
 * <p>Every property is always present; {@code owner_profile} and {@code dashboard_config} are
 * {@code null} when the account has none. It never carries a token.</p>
 *
 * @param user            the signed-in account
 * @param ownerProfile    the owner profile, with its address parts; {@code null} for a technician
 * @param dashboardConfig the dashboard configuration; {@code null} while it is not set up
 * @param counts          how many sites and units the owner has
 * @param serverTime      the server instant of the read, for the client to compute its clock skew
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(
    name = "SessionContextResponse",
    description = "Session bootstrap: the signed-in account, its owner profile and dashboard configuration, its "
        + "site and unit counts, and the server time",
    example = "{\"user\": {\"id\": 42, \"username\": \"maria\", \"email\": \"maria.lopez@example.com\", "
        + "\"role\": \"OWNER_ROLE\", \"provider\": \"LOCAL\"}, \"owner_profile\": {\"id\": 3, \"user_id\": 42, "
        + "\"full_name\": \"Maria Lopez\", \"email\": \"maria.lopez@example.com\", \"phone\": \"+51 987654321\", "
        + "\"address\": \"Av. Primavera 123, Lima, 15023, Peru\", \"street\": \"Av. Primavera\", \"number\": \"123\", "
        + "\"city\": \"Lima\", \"postal_code\": \"15023\", \"country\": \"Peru\", \"ruc\": 20123456789, "
        + "\"version\": 0}, \"dashboard_config\": null, \"counts\": {\"sites\": 0, \"equipment\": 0}, "
        + "\"server_time\": \"2026-11-09T15:02:40Z\"}")
public record SessionContextResource(
    @Schema(description = "The signed-in account")
    SessionUserResource user,

    @Schema(description = "The owner profile; null for a technician", nullable = true)
    OwnerProfileResource ownerProfile,

    @Schema(description = "The dashboard configuration; null while it is not set up", nullable = true)
    DashboardConfigResource dashboardConfig,

    @Schema(description = "How many sites and units the owner has; both 0 for a technician")
    SessionCountsResource counts,

    @Schema(description = "Server time of the read, UTC", example = "2026-11-09T15:02:40Z")
    Instant serverTime
) {
}
