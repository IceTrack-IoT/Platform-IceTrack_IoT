package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.Intervention;

/**
 * Outcome of submitting an intervention.
 *
 * <p>Carries whether the submission was new or a replay of one already registered, which the REST
 * layer answers with {@code 201 Created} and {@code 200 OK} respectively.</p>
 *
 * @param serviceRequest the completed request holding the intervention; required
 * @param replayed       {@code true} when the request already held an intervention with the
 *                       submitted {@code clientGeneratedId}, so nothing was written
 */
public record InterventionRegistration(ServiceRequest serviceRequest, boolean replayed) {

  /**
   * Validates the outcome.
   *
   * @throws IllegalArgumentException when the request is missing or holds no intervention
   */
  public InterventionRegistration {
    if (serviceRequest == null || serviceRequest.getIntervention() == null) {
      throw new IllegalArgumentException("An intervention registration requires a request holding an intervention");
    }
  }

  /**
   * Returns the registered intervention.
   *
   * @return the intervention of the request
   */
  public Intervention intervention() {
    return serviceRequest.getIntervention();
  }
}
