package pe.edu.upc.ice.track.platform.shared.infrastructure.time;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * The one rule by which the platform reads a moment sent by a client, a mobile app or an edge gateway.
 *
 * <p>A moment is an ISO 8601 date-time that states its offset: {@code 2026-11-08T13:30:00Z} or
 * {@code 2026-11-08T08:30:00-05:00}, fractional seconds allowed. It is turned into an {@link Instant},
 * so an offset only says how the sender wrote it; every moment the platform stores and returns is UTC,
 * written with {@code Z}.</p>
 *
 * <p>A date-time without an offset is refused: it names a wall-clock time whose zone the platform
 * would have to guess, which is how timestamps silently shifted by hours. A number is refused too,
 * because seconds and milliseconds since the epoch cannot be told apart.</p>
 */
public final class IsoInstants {

  /** What a refused value is told it should look like. */
  public static final String EXPECTED_FORMAT =
      "an ISO 8601 date-time with Z or an explicit offset, such as 2026-11-08T13:30:00Z";

  private IsoInstants() {
  }

  /**
   * @param text the value sent; surrounding blanks are ignored
   * @return the moment it denotes
   * @throws IllegalArgumentException when the value is blank, has no offset or is not ISO 8601
   */
  public static Instant parse(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("A date-time is required: " + EXPECTED_FORMAT);
    }
    try {
      return OffsetDateTime.parse(text.trim(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("'%s' is not %s".formatted(text.trim(), EXPECTED_FORMAT), e);
    }
  }
}
