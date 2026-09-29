package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

/**
 * Command to create or update an alert policy, optionally scoped to a single equipment.
 *
 * @param equipmentId                  identifier of the equipment this policy applies to;
 *                                     {@code null} targets the platform-wide default policy
 * @param sustainedExcursionMinutes    minutes an out-of-range reading must persist before
 *                                     an alert opens
 * @param hysteresisMarginCelsius      margin, in Celsius, a reading must re-enter range by
 *                                     before the alert closes
 * @param missedSyncWindowsForOffline  number of missed synchronisation windows before a
 *                                     device is considered offline
 */
public record UpdateAlertPolicyCommand(
    Long equipmentId,
    Integer sustainedExcursionMinutes,
    Double hysteresisMarginCelsius,
    Integer missedSyncWindowsForOffline) {
}
