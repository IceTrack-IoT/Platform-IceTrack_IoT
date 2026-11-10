package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

import java.util.Locale;
import java.util.Objects;

/**
 *  Value object representing a single named, typed metric produced by a
 *  {@code ReportGenerationStrategy}.
 *
 *  <p>A {@code Report}'s {@code resultPayload} is the JSON serialization of the
 *  {@code List<KpiValue>} its strategy produced -- this is the one type every strategy's output
 *  has in common, whatever report type it belongs to.</p>
 *
 *  <p>The {@code key} is the stable, machine-readable name of the metric, so a client can translate
 *  the label instead of showing the English one. A metric computed for one unit carries the
 *  {@value #UNIT_KEY_SEPARATOR}{@code unit_<equipmentId>} suffix, built by {@link #perUnitKey}, so
 *  the part before the separator is always one of the documented metric keys. A payload stored
 *  before keys existed has none; such a value gets a key derived from its label instead, see
 *  {@link #keyFromLabel}.</p>
 *
 * @param key   the metric's stable machine key, e.g. {@code "excursion_count"} or
 *              {@code "uptime_pct.unit_10"}; derived from the label when {@code null} or blank
 * @param label the metric's display name, e.g. {@code "Uptime"}
 * @param value the metric's numeric value
 * @param unit  the metric's unit, e.g. {@code "%"} or {@code "h"}; required, use {@code ""} for a
 *              dimensionless count
 */
public record KpiValue(String key, String label, Double value, String unit) {

  /** Separates a metric key from the unit it was computed for. */
  public static final String UNIT_KEY_SEPARATOR = ".";

  public KpiValue {
    Objects.requireNonNull(label, "label must not be null");
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(unit, "unit must not be null");
    key = key == null || key.isBlank() ? keyFromLabel(label) : key;
  }

  /**
   * Builds the key of a metric computed for a single unit.
   *
   * @param metricKey   the documented metric key, e.g. {@code "excursion_count"}
   * @param equipmentId the unit the metric was computed for
   * @return the key, e.g. {@code "excursion_count.unit_10"}
   */
  public static String perUnitKey(String metricKey, Long equipmentId) {
    return metricKey + UNIT_KEY_SEPARATOR + "unit_" + equipmentId;
  }

  /**
   * Derives a key from a label, for a value stored before keys existed: lower case, with every run
   * of characters other than letters and digits replaced by one underscore, for example
   * {@code "Total excursions"} becomes {@code "total_excursions"}.
   *
   * @param label the label
   * @return the derived key, never blank
   */
  public static String keyFromLabel(String label) {
    var derived = label.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "_")
        .replaceAll("^_+|_+$", "");
    return derived.isEmpty() ? "kpi" : derived;
  }
}
