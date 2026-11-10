package pe.edu.upc.ice.track.platform.device.application.results;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The configuration an edge gateway enforces for its unit, as served by its configuration poll.
 *
 * <p>Composed from three contexts: the threshold and the version from Assets Management, the alert
 * policy from Monitoring and Alerting, and the technician grants from Service Request Management.
 * Device Management owns none of it.</p>
 *
 * @param configVersion               version of this configuration, the ETag of the poll
 * @param minCelsius                  lowest acceptable temperature
 * @param maxCelsius                  highest acceptable temperature
 * @param sustainedExcursionMinutes   minutes an excursion must last before it is raised
 * @param hysteresisMarginCelsius     recovery margin, in Celsius, before an excursion closes
 * @param missedSyncWindowsForOffline missed synchronization windows before a device counts as offline
 * @param grants                      the technician grants the gateway must accept
 */
public record EdgeConfiguration(
    int configVersion,
    double minCelsius,
    double maxCelsius,
    int sustainedExcursionMinutes,
    double hysteresisMarginCelsius,
    int missedSyncWindowsForOffline,
    List<Grant> grants) {

  public EdgeConfiguration {
    grants = List.copyOf(Objects.requireNonNull(grants, "grants must not be null"));
  }

  /**
   * A technician grant the gateway must accept.
   *
   * @param tokenSha256         hex SHA-256 digest of the grant token
   * @param technicianProfileId the technician it was issued to
   * @param serviceRequestId    the request it was issued for
   * @param expiresAt           when it stops being valid
   */
  public record Grant(String tokenSha256, Long technicianProfileId, Long serviceRequestId, Instant expiresAt) {
  }
}
