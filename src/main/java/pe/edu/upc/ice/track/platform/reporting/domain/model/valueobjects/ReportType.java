package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

/**
 *  Enumeration representing the kind of report being requested.
 *
 *  <p>Each constant is handled by exactly one {@code ReportGenerationStrategy} bean, keyed by
 *  this enum's name -- see {@code ReportCommandServiceImpl} for how the two are wired.</p>
 *
 *  <p>{@link #SERVICE_INTERVENTION} documents one completed service request rather than a period:
 *  it is the only type scoped by a service request, and its period is the request's own.</p>
 */
public enum ReportType {
  MAINTENANCE_COMPLIANCE,
  EQUIPMENT_UPTIME,
  TECHNICIAN_PERFORMANCE,
  TEMPERATURE_EXCURSION,
  SERVICE_INTERVENTION;

  /**
   * Tells whether this type documents a single service request.
   *
   * @return {@code true} only for {@link #SERVICE_INTERVENTION}
   */
  public boolean isScopedByServiceRequest() {
    return this == SERVICE_INTERVENTION;
  }
}
