package pe.edu.upc.ice.track.platform.monitoring.interfaces.acl;

/**
 * Published view of the alert policy that applies to a unit, returned by
 * {@link MonitoringContextFacade#fetchApplicableAlertPolicy(Long)}.
 *
 * <p>Part of the published language of the {@code monitoring} context: plain values only, so no
 * {@code AlertPolicy} aggregate or value object leaves the context.</p>
 *
 * @param sustainedExcursionMinutes   minutes an excursion must last before it is raised
 * @param hysteresisMarginCelsius     margin, in Celsius, the temperature must recover by relative to
 *                                    the breached limit before an excursion closes
 * @param missedSyncWindowsForOffline missed synchronization windows before a device counts as offline
 */
public record AlertPolicySettings(
    int sustainedExcursionMinutes,
    double hysteresisMarginCelsius,
    int missedSyncWindowsForOffline) {
}
