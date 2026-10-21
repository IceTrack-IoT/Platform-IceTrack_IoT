package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

import java.util.Objects;

/**
 *  Value object representing a single named, typed metric produced by a
 *  {@code ReportGenerationStrategy}.
 *
 *  <p>A {@code Report}'s {@code resultPayload} is the JSON serialization of the
 *  {@code List<KpiValue>} its strategy produced -- this is the one type every strategy's output
 *  has in common, whatever report type it belongs to.</p>
 *
 * @param label the metric's display name, e.g. {@code "Uptime"}
 * @param value the metric's numeric value
 * @param unit  the metric's unit, e.g. {@code "%"} or {@code "h"}; required, use {@code ""} for a
 *              dimensionless count
 */
public record KpiValue(String label, Double value, String unit) {

  public KpiValue {
    Objects.requireNonNull(label, "label must not be null");
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(unit, "unit must not be null");
  }
}
