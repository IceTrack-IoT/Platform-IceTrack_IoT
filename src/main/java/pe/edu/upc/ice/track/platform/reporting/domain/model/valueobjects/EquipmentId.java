package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

/**
 *  Value object representing the identifier of an equipment unit, as referenced by Reporting.
 *
 *  <p>Deliberately local to this context rather than imported from {@code assets}, the same way
 *  {@code monitoring} and {@code notifications} each own their own {@code EquipmentId}: a
 *  Bounded Context only ever holds another context's identifier, never its aggregate.</p>
 *
 * @param equipmentId the identifier of the equipment; may be {@code null} when a report's
 *                     {@code ReportFilters} does not narrow to a single unit
 */
@Embeddable
public record EquipmentId(Long equipmentId) {

  public EquipmentId{
    if (equipmentId == null){
      throw new IllegalArgumentException("EquipmentId cannot be null");
    }
  }
}
