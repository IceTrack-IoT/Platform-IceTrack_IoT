package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

/**
 * Command to create or update the alert policy of a single equipment.
 *
 * <p>The platform-wide default policy is not reachable through this command: it is seeded from
 * configuration at startup ({@link SeedDefaultAlertPolicyCommand}), so no tenant can change the
 * detection settings every other tenant is evaluated with.</p>
 *
 * <p>A policy carries no temperature band: a unit's thresholds are owned by Assets Management
 * and changed only through its own threshold endpoint.</p>
 *
 * @param equipmentId                  identifier of the equipment this policy applies to; required
 * @param username                     username of the authenticated caller, who must own the
 *                                     equipment; required
 * @param sustainedExcursionMinutes    minutes an out-of-range reading must persist before
 *                                     an alert opens
 * @param hysteresisMarginCelsius      margin, in Celsius, a reading must re-enter range by
 *                                     before the alert closes
 * @param missedSyncWindowsForOffline  number of missed synchronization windows before a
 *                                     device is considered offline
 */
public record UpdateAlertPolicyCommand(
    Long equipmentId,
    String username,
    Integer sustainedExcursionMinutes,
    Double hysteresisMarginCelsius,
    Integer missedSyncWindowsForOffline) {
}
