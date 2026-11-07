package pe.edu.upc.ice.track.platform.monitoring.application.results;

import java.time.Duration;

/**
 * A stream ticket just issued, returned to the client once and never stored in this form.
 *
 * @param ticket    the opaque ticket value, {@code st_} followed by 64 hexadecimal characters
 * @param expiresIn how long the ticket stays redeemable
 */
public record IssuedStreamTicket(String ticket, Duration expiresIn) {

  /** How long every stream ticket stays redeemable after it is issued. */
  public static final Duration TTL = Duration.ofSeconds(60);
}
