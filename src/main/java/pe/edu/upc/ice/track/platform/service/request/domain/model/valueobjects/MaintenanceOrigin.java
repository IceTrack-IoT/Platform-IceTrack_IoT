package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

/**
 * How a maintenance window linked to a service request was started on the unit's gateway, in this
 * context's own vocabulary: Monitoring and Alerting owns the window and publishes its origin as
 * plain text.
 */
public enum MaintenanceOrigin {

  /** Started from the mobile app by the assigned technician. */
  APP,

  /** Started with the hardware button of the gateway. */
  BUTTON
}
