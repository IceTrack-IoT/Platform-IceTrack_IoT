package pe.edu.upc.ice.track.platform.service.request.domain.model.commands;

import java.time.Instant;

/**
 * Command to start the work on a service request assigned to the caller.
 *
 * @param serviceRequestId identifier of the request; required
 * @param username         username of the authenticated caller, resolved by the command service to
 *                         the technician the request must be assigned to; required
 * @param startedAt        when the work started according to the technician's device; {@code null}
 *                         uses the server clock
 */
public record StartServiceRequestCommand(
    Long serviceRequestId,
    String username,
    Instant startedAt) {
}
