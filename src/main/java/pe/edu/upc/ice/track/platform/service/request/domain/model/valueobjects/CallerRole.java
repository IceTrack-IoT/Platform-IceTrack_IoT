package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

/**
 * The capacity in which an authenticated caller reads this context.
 *
 * <p>Only the reads open to both roles carry it. An owner reaches the requests it created; a
 * technician reaches the requests assigned to it. Neither ever reaches a request of somebody
 * else, and such a request is reported as missing rather than forbidden.</p>
 */
public enum CallerRole {

  /** Reaches only the service requests it created, and the reviews it wrote. */
  OWNER,

  /** Reaches only the service requests assigned to it, and the reviews about it. */
  TECHNICIAN
}
