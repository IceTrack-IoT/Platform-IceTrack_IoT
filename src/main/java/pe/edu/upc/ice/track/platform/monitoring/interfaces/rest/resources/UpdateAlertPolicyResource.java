package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

/** Inbound REST resource used to create or update an alert policy. */
public record UpdateAlertPolicyResource(
    Long equipmentId,
    Integer sustainedExcursionMinutes,
    Double hysteresisMarginCelsius,
    Integer missedSyncWindowsForOffline) {
}
