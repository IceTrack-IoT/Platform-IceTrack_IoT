package pe.edu.upc.ice.track.platform.service.request.interfaces.acl;

import java.time.Instant;

/**
 * Published view of an edge grant that currently opens a unit's gateway, returned by
 * {@link ServiceRequestContextFacade#fetchActiveEdgeGrants(Long)}.
 *
 * <p>Part of the published language of the {@code service.request} context: plain values only,
 * and never the plaintext token, only its digest.</p>
 *
 * @param tokenSha256         hex SHA-256 digest of the grant token
 * @param technicianProfileId the technician the grant was issued to
 * @param serviceRequestId    the request it was issued for
 * @param expiresAt           when it stops being valid
 */
public record ActiveEdgeGrant(String tokenSha256, Long technicianProfileId, Long serviceRequestId, Instant expiresAt) {
}
