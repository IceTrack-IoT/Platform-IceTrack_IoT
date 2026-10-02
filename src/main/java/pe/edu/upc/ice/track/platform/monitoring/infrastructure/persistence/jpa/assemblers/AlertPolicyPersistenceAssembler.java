package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.AlertPolicyPersistenceEntity;

/** Bidirectional mapper between {@link AlertPolicy} and {@link AlertPolicyPersistenceEntity}. */
public final class AlertPolicyPersistenceAssembler {

  private AlertPolicyPersistenceAssembler() {
  }

  public static AlertPolicyPersistenceEntity toPersistenceEntityFromDomain(AlertPolicy policy) {
    var entity = new AlertPolicyPersistenceEntity();
    entity.setId(policy.getId());
    entity.setEquipmentId(policy.getEquipmentId().equipmentId());
    entity.setSustainedExcursionMinutes(policy.getSustainedExcursionMinutes());
    entity.setHysteresisMarginCelsius(policy.getHysteresisMargin().celsius());
    entity.setMissedSyncWindowsForOffline(policy.getMissedSyncWindowsForOffline());
    entity.setActive(policy.isActive());
    return entity;
  }

  public static AlertPolicy toDomainFromPersistenceEntity(AlertPolicyPersistenceEntity entity) {
    var policy = new AlertPolicy(
        new EquipmentId(entity.getEquipmentId()),
        entity.getSustainedExcursionMinutes(),
        entity.getHysteresisMarginCelsius(), entity.getMissedSyncWindowsForOffline());
    policy.assignId(entity.getId());
    if (!entity.isActive()) {
      policy.deactivate();
    }
    return policy;
  }
}
