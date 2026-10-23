package pe.edu.upc.ice.track.platform.service.request.domain.model.commands;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Command to register the intervention that completes a service request assigned to the caller.
 *
 * <p>Idempotent on {@code clientGeneratedId}: submitting it again for the same request returns
 * the intervention already registered instead of failing.</p>
 *
 * @param serviceRequestId  identifier of the request; required
 * @param username          username of the authenticated caller, resolved by the command service
 *                          to the technician the request must be assigned to; required
 * @param clientGeneratedId the identifier the technician's device generated for this report;
 *                          required
 * @param diagnosis         what the technician found; required
 * @param actionsTaken      what the technician did; required
 * @param spareParts        the spare parts used, may be {@code null} or empty
 * @param photoEvidence     references to the photos taken, may be {@code null} or empty
 * @param completedAt       when the work finished according to the device; {@code null} uses the
 *                          server clock
 */
public record RegisterInterventionCommand(
    Long serviceRequestId,
    String username,
    UUID clientGeneratedId,
    String diagnosis,
    String actionsTaken,
    List<String> spareParts,
    List<String> photoEvidence,
    Instant completedAt) {
}
