package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.TemperatureThreshold;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.entities.EquipmentPersistenceEntity;

/**
 * Static assembler between the {@link Equipment} aggregate and its
 * {@link EquipmentPersistenceEntity}.
 *
 * <p>The temperature band is split into its two columns on the way out and joined back into a
 * single {@link TemperatureThreshold} on the way in, so the invariant {@code min < max} is
 * re-established by the value object on every read rather than trusted from the row.</p>
 */
public final class EquipmentPersistenceAssembler {

  private EquipmentPersistenceAssembler() {
  }

  /**
   * Rebuilds the aggregate from a persisted row.
   *
   * @param entity the persisted row; may be {@code null}
   * @return the aggregate, or {@code null} when there is no row
   */
  public static Equipment toDomainFromPersistence(EquipmentPersistenceEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Equipment(
        entity.getId(),
        entity.getSiteId(),
        entity.getEquipmentUid(),
        entity.getName(),
        entity.getEquipmentType(),
        entity.getStatus(),
        entity.isOnline(),
        new TemperatureThreshold(entity.getThresholdMinCelsius(), entity.getThresholdMaxCelsius()),
        entity.getReminderIntervalDays(),
        entity.getLastReadingAt(),
        entity.getLastKnownTemperature());
  }

  /**
   * Builds the persisted row of an aggregate.
   *
   * @param equipment the aggregate; may be {@code null}
   * @return the row to hand to Spring Data
   */
  public static EquipmentPersistenceEntity toPersistenceFromDomain(Equipment equipment) {
    if (equipment == null) {
      return null;
    }
    var entity = new EquipmentPersistenceEntity();
    entity.setId(equipment.getEquipmentId());
    entity.setSiteId(equipment.getSiteId());
    entity.setEquipmentUid(equipment.getUid());
    entity.setName(equipment.getName());
    entity.setEquipmentType(equipment.getEquipmentType());
    entity.setStatus(equipment.getStatus());
    entity.setOnline(equipment.isOnline());
    entity.setReminderIntervalDays(equipment.getReminderIntervalDays());
    entity.setThresholdMinCelsius(equipment.getTemperatureThreshold().minCelsius());
    entity.setThresholdMaxCelsius(equipment.getTemperatureThreshold().maxCelsius());
    entity.setLastReadingAt(equipment.getLastReadingAt());
    entity.setLastKnownTemperature(equipment.getLastKnownTemperature());
    return entity;
  }
}