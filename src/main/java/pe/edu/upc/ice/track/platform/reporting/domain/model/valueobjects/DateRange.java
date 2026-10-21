package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 *  Value object representing the closed date-time window a report is computed over.
 *
 *  <p>Named {@code DateRange} here -- the diagram this bounded context was built from spells it
 *  {@code DataRange}, which reads as a typo for a date interval; the class keeps the corrected
 *  name since nothing else in this codebase uses the misspelling.</p>
 *
 * @param from inclusive start of the window; required
 * @param to   inclusive end of the window; required, not before {@code from}
 */
public record DateRange(LocalDateTime from, LocalDateTime to) {

  public DateRange {
    Objects.requireNonNull(from, "from must not be null");
    Objects.requireNonNull(to, "to must not be null");
    if (from.isAfter(to)) {
      throw new IllegalArgumentException("from must not be after to");
    }
  }

  /** @return the window length in hours, used to turn an absolute duration into a percentage */
  public double totalHours() {
    return Duration.between(from, to).toSeconds() / 3600.0;
  }
}
