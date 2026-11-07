package pe.edu.upc.ice.track.platform.monitoring.interfaces.events;

/**
 * Integration event published when the alert policy of a unit is created or changed.
 *
 * <p>The published language of the {@code monitoring} context for this fact. Assets Management
 * subscribes to it to increment the unit's edge configuration version, so the gateway downloads
 * the new policy on its next poll. Other contexts must subscribe to this event, never to the
 * internal {@code AlertPolicyUpdatedEvent}.</p>
 *
 * @param equipmentId                 the unit the policy applies to
 * @param sustainedExcursionMinutes   minutes an excursion must last before it is raised
 * @param hysteresisMarginCelsius     margin the temperature must recover by before an excursion closes
 * @param missedSyncWindowsForOffline missed synchronization windows before a device counts as offline
 */
public record AlertPolicyUpdatedIntegrationEvent(
    Long equipmentId,
    Integer sustainedExcursionMinutes,
    Double hysteresisMarginCelsius,
    Integer missedSyncWindowsForOffline) {
}
