package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.projections;

/**
 * Closed projection of a {@code ServiceRequestPersistenceEntity} onto its unit identifier.
 *
 * <p>Being closed, it lets Spring Data select the {@code equipment_id} column alone instead of
 * the whole row, so a {@code Distinct} derived query applies to the identifiers themselves.</p>
 */
public interface EquipmentIdProjection {

  /** @return the identifier of the unit the request is about */
  Long getEquipmentId();
}
