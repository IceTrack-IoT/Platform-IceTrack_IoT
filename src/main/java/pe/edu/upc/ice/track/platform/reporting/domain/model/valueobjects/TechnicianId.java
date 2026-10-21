package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

/**
 *  Value object representing the identifier of a technician, as referenced by Reporting.
 * @param technicianId  the identifier of the technician; may be {@code null} when a report's
 *                      {@code ReportFilters} does not narrow to a single technician
 */
@Embeddable
public record TechnicianId(Long technicianId) {
  public TechnicianId{
    if (technicianId == null){
      throw new IllegalArgumentException("TechnicianId cannot be null");
    }
  }
}
