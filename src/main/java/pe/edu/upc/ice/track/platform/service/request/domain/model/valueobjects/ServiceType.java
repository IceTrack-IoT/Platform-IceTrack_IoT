package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

/**
 * Kind of work a service request asks a technician for.
 */
public enum ServiceType {

  /** Scheduled visit on a working unit, the one the maintenance interval of the equipment plans. */
  PREVENTIVE_MAINTENANCE,

  /** Corrective visit on a unit with a fault. */
  REPAIR
}
