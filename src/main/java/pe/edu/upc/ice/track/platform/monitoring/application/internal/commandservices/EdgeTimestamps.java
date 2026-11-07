package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Converts the zone-less timestamps edge gateways report into instants.
 *
 * <p>Gateways report {@code occurred_at} and {@code recorded_at} without an offset, on the same clock
 * basis as the server's own {@code LocalDateTime.now()}, which is what alerts and readings are
 * compared against. They are therefore read in the server's default zone, the same convention the
 * reporting context applies when it turns those local timestamps into instants.</p>
 */
final class EdgeTimestamps {

  private EdgeTimestamps() {
  }

  /**
   * @param localDateTime a timestamp reported by a gateway; may be {@code null}
   * @return the instant it denotes, or {@code null} when none was given
   */
  static Instant toInstant(LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
  }
}
