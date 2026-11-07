package pe.edu.upc.ice.track.platform.monitoring.domain.model.events;

/**
 * Domain event registered when the alert policy of a unit is created or changed.
 *
 * <p>The edge gateway of the unit enforces the policy locally, so the change must reach it on its
 * next configuration poll.</p>
 *
 * @param equipmentId                 the unit the policy applies to
 * @param sustainedExcursionMinutes   minutes an excursion must last before it is raised
 * @param hysteresisMarginCelsius     margin the temperature must recover by before an excursion closes
 * @param missedSyncWindowsForOffline missed synchronization windows before a device counts as offline
 */
public record AlertPolicyUpdatedEvent(
    Long equipmentId,
    Integer sustainedExcursionMinutes,
    Double hysteresisMarginCelsius,
    Integer missedSyncWindowsForOffline) {
}
