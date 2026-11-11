package pe.edu.upc.ice.track.platform.profiles.application.results;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;

import java.time.Instant;
import java.util.Objects;

/**
 * Everything a client shell needs right after sign-in, read in one go.
 *
 * <p>Never carries a token or any other credential.</p>
 *
 * @param userId          identifier of the signed-in account
 * @param username        the username it signed in with
 * @param email           its email address, as recorded on its profile; {@code null} without a profile
 * @param role            its role name
 * @param provider        its identity provider name
 * @param ownerProfile    its owner profile; {@code null} for a technician or an account without one
 * @param dashboardConfig its dashboard configuration; {@code null} while it is not set up
 * @param siteCount       how many sites the owner has; {@code 0} for a technician
 * @param equipmentCount  how many units the owner has; {@code 0} for a technician
 * @param serverTime      the server instant of the read, for the client to compute its clock skew
 */
public record SessionContext(
    Long userId,
    String username,
    String email,
    String role,
    String provider,
    OwnerProfile ownerProfile,
    DashboardConfig dashboardConfig,
    long siteCount,
    long equipmentCount,
    Instant serverTime) {

  /**
   * Validates the values every context carries.
   */
  public SessionContext {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(serverTime, "serverTime must not be null");
  }
}
