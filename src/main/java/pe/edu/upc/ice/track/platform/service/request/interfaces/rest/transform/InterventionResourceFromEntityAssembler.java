package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.Intervention;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.InterventionResource;

import java.time.Instant;

/**
 * Assembler to convert an {@link Intervention} to an {@link InterventionResource}.
 */
public class InterventionResourceFromEntityAssembler {

  /**
   * Converts the intervention of a completed {@link ServiceRequest} to an
   * {@link InterventionResource}.
   *
   * @param serviceRequest the completed request, holding its intervention
   * @return the {@link InterventionResource} resource
   */
  public static InterventionResource toResourceFromEntity(ServiceRequest serviceRequest) {
    return toResourceFromIntervention(serviceRequest.getId().serviceRequestId(), serviceRequest.getIntervention());
  }

  /**
   * Converts an {@link Intervention} to an {@link InterventionResource}.
   *
   * @param serviceRequestId identifier of the request the intervention completed
   * @param intervention     the intervention to convert
   * @return the {@link InterventionResource} resource
   */
  public static InterventionResource toResourceFromIntervention(Long serviceRequestId, Intervention intervention) {
    return new InterventionResource(
        serviceRequestId,
        intervention.getClientGeneratedId(),
        intervention.getTechnicianId().technicianProfileId(),
        intervention.getEquipmentId().equipmentId(),
        intervention.getDiagnosis(),
        intervention.getActionsTaken(),
        intervention.getSpareParts(),
        intervention.getPhotoEvidence(),
        toIsoString(intervention.getStartedAt()),
        toIsoString(intervention.getCompletedAt()),
        intervention.duration().toSeconds());
  }

  private static String toIsoString(Instant value) {
    return value == null ? null : value.toString();
  }
}
