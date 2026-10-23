package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.Intervention;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.StatusTransition;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.InterventionPersistenceEntity;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.ServiceRequestPersistenceEntity;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.StatusTransitionPersistenceEntity;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Static assembler between the {@link ServiceRequest} aggregate and its
 * {@link ServiceRequestPersistenceEntity}, history and intervention included.
 *
 * <p>The aggregate is rebuilt through its constructors on every read, so its invariants are
 * re-established rather than trusted from the row. On the way out, the state is copied onto an
 * entity that is either new or the managed entity the request was loaded from, which is what keeps
 * the managed {@code history} collection in place and lets dirty checking write exactly what
 * changed.</p>
 */
public final class ServiceRequestPersistenceAssembler {

  private ServiceRequestPersistenceAssembler() {
  }

  /**
   * Rebuilds the aggregate from a persisted row.
   *
   * @param entity the persisted row; may be {@code null}
   * @return the aggregate, or {@code null} when there is no row
   */
  public static ServiceRequest toDomainFromPersistence(ServiceRequestPersistenceEntity entity) {
    if (entity == null) {
      return null;
    }
    return new ServiceRequest(
        new ServiceRequestId(entity.getId()),
        new OwnerId(entity.getOwnerId()),
        new EquipmentId(entity.getEquipmentId()),
        new SiteId(entity.getSiteId()),
        toTechnicianId(entity.getTechnicianId()),
        entity.getType(),
        entity.getPriority(),
        entity.getDescription(),
        entity.getStatus(),
        entity.getRequestedAt(),
        entity.getAssignedAt(),
        entity.getStartedAt(),
        entity.getCompletedAt(),
        entity.getCanceledAt(),
        entity.getCancelReason(),
        entity.getVersion(),
        entity.getHistory().stream()
            .map(ServiceRequestPersistenceAssembler::toTransitionFromPersistence)
            .toList(),
        toInterventionFromPersistence(entity.getIntervention()));
  }

  /**
   * Copies the state of a request onto its persistence entity, which is either new or the managed
   * entity the request was loaded from.
   *
   * <p>The history is append-only, so transitions are reconciled by identity and never updated: a
   * transition without an identity is appended, and a transition with one must already be a row of
   * this entity. The intervention is immutable, so it is attached once and never replaced.</p>
   *
   * @param serviceRequest the request
   * @param entity         the persistence entity to update
   * @throws IllegalStateException when the aggregate holds a persisted transition this entity does
   *                               not, or drops or replaces its intervention
   */
  public static void copyToPersistence(ServiceRequest serviceRequest, ServiceRequestPersistenceEntity entity) {
    entity.setOwnerId(serviceRequest.getOwnerId().ownerId());
    entity.setEquipmentId(serviceRequest.getEquipmentId().equipmentId());
    entity.setSiteId(serviceRequest.getSiteId().siteId());
    entity.setTechnicianId(serviceRequest.getTechnicianId() == null
        ? null
        : serviceRequest.getTechnicianId().technicianProfileId());
    entity.setType(serviceRequest.getType());
    entity.setPriority(serviceRequest.getPriority());
    entity.setDescription(serviceRequest.getDescription());
    entity.setStatus(serviceRequest.getStatus());
    entity.setRequestedAt(serviceRequest.getCreatedAt());
    entity.setAssignedAt(serviceRequest.getAssignedAt());
    entity.setStartedAt(serviceRequest.getStartedAt());
    entity.setCompletedAt(serviceRequest.getCompletedAt());
    entity.setCanceledAt(serviceRequest.getCanceledAt());
    entity.setCancelReason(serviceRequest.getCancelReason());
    copyHistory(serviceRequest, entity);
    copyIntervention(serviceRequest, entity);
  }

  private static void copyHistory(ServiceRequest serviceRequest, ServiceRequestPersistenceEntity entity) {
    var persistedTransitionIds = entity.getHistory().stream()
        .map(StatusTransitionPersistenceEntity::getId)
        .collect(Collectors.toSet());
    for (var transition : serviceRequest.getHistory()) {
      if (transition.getId() == null) {
        entity.addTransition(toTransitionPersistence(transition));
      } else if (!persistedTransitionIds.contains(transition.getId())) {
        throw new IllegalStateException("Status transition %s does not belong to service request %s"
            .formatted(transition.getId(), entity.getId()));
      }
    }
  }

  private static void copyIntervention(ServiceRequest serviceRequest, ServiceRequestPersistenceEntity entity) {
    var intervention = serviceRequest.getIntervention();
    var persistedIntervention = entity.getIntervention();
    if (intervention == null) {
      if (persistedIntervention != null) {
        throw new IllegalStateException(
            "The intervention of service request %s cannot be removed".formatted(entity.getId()));
      }
      return;
    }
    if (persistedIntervention == null) {
      entity.setIntervention(toInterventionPersistence(intervention));
      return;
    }
    if (!intervention.isIdentifiedBy(persistedIntervention.getClientGeneratedId())) {
      throw new IllegalStateException(
          "The intervention of service request %s cannot be replaced".formatted(entity.getId()));
    }
  }

  private static StatusTransition toTransitionFromPersistence(StatusTransitionPersistenceEntity entity) {
    return new StatusTransition(
        entity.getId(),
        entity.getFromStatus(),
        entity.getToStatus(),
        entity.getActorId(),
        entity.getOccurredAt(),
        entity.getReason(),
        toTechnicianId(entity.getTechnicianId()));
  }

  private static StatusTransitionPersistenceEntity toTransitionPersistence(StatusTransition transition) {
    var entity = new StatusTransitionPersistenceEntity();
    entity.setFromStatus(transition.getFromStatus());
    entity.setToStatus(transition.getToStatus());
    entity.setActorId(transition.getActorId());
    entity.setOccurredAt(transition.getOccurredAt());
    entity.setReason(transition.getReason());
    entity.setTechnicianId(transition.getTechnicianId() == null
        ? null
        : transition.getTechnicianId().technicianProfileId());
    return entity;
  }

  private static Intervention toInterventionFromPersistence(InterventionPersistenceEntity entity) {
    if (entity == null) {
      return null;
    }
    return new Intervention(
        entity.getClientGeneratedId(),
        new TechnicianProfileId(entity.getTechnicianId()),
        new EquipmentId(entity.getEquipmentId()),
        entity.getDiagnosis(),
        entity.getActionsTaken(),
        entity.getSpareParts(),
        entity.getPhotoEvidence(),
        entity.getStartedAt(),
        entity.getCompletedAt());
  }

  private static InterventionPersistenceEntity toInterventionPersistence(Intervention intervention) {
    var entity = new InterventionPersistenceEntity();
    entity.setClientGeneratedId(intervention.getClientGeneratedId());
    entity.setTechnicianId(intervention.getTechnicianId().technicianProfileId());
    entity.setEquipmentId(intervention.getEquipmentId().equipmentId());
    entity.setDiagnosis(intervention.getDiagnosis());
    entity.setActionsTaken(intervention.getActionsTaken());
    entity.setSpareParts(new ArrayList<>(intervention.getSpareParts()));
    entity.setPhotoEvidence(new ArrayList<>(intervention.getPhotoEvidence()));
    entity.setStartedAt(intervention.getStartedAt());
    entity.setCompletedAt(intervention.getCompletedAt());
    return entity;
  }

  private static TechnicianProfileId toTechnicianId(Long technicianId) {
    return technicianId == null ? null : new TechnicianProfileId(technicianId);
  }
}
