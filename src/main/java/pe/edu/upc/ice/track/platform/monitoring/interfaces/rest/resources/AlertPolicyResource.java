package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

/** Outbound REST resource representing an alert policy. */
public record AlertPolicyResource(
    Long id,
    Long equipmentId,
    Integer sustainedExcursionMinutes,
    Double hysteresisMarginCelsius,
    Integer missedSyncWindowsForOffline,
    boolean active) {
}
