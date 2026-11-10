package pe.edu.upc.ice.track.platform.device.domain.model.valueobjects;

/**
 * Lifecycle state of an IoT board managed by the platform.
 *
 * <pre>
 *            pair (new key)              revoke
 *   UNPAIRED ---------------&gt; PAIRED -----------&gt; REVOKED
 *      ^                       |  ^                  |
 *      |        unpair         |  |  rotate API key  |
 *      +-----------------------+  +------------------+
 *      ^                                             |
 *      +-------------------- unpair -----------------+
 * </pre>
 *
 * <p>Each state fixes what the board holds, and {@code Device} re-checks those invariants every
 * time it is built:</p>
 * <ul>
 *   <li>{@link #UNPAIRED}: no equipment and no API key digest;</li>
 *   <li>{@link #PAIRED}: one equipment and one API key digest;</li>
 *   <li>{@link #REVOKED}: still linked to its equipment, but with no API key digest.</li>
 * </ul>
 */
public enum DeviceStatus {
  /** Not linked to any refrigeration unit and holding no credential; free to be paired. */
  UNPAIRED,
  /** Linked to one refrigeration unit and allowed to authenticate with its API key. */
  PAIRED,
  /**
   * Credential withdrawn while the board stays linked to its unit. Every request with the old key
   * is rejected. Issuing a new key resumes the pairing, and unpairing frees the board.
   */
  REVOKED
}
