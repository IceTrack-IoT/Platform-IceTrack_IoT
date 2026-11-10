package pe.edu.upc.ice.track.platform.monitoring.domain.model.readmodels;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Read model of an owner's dashboard: the handful of figures its overview cards show, computed on
 * the server in one pass.
 *
 * <p>It exists so that no client has to rebuild these totals from the listings - every unit, every
 * alert, every policy - only to reduce them to a few numbers. Nothing is stored for it: the figures
 * are counted when they are asked for, so they are as fresh as {@code generatedAt} says.</p>
 *
 * <p>Whether a unit is reporting is judged here, against the offline window of the policy that
 * applies to it and by the server clock, so every client shows the same split whatever its own clock
 * says.</p>
 *
 * @param generatedAt     when the figures were computed
 * @param siteCount       sites the figures cover: the owner's, or one when narrowed to a site
 * @param equipment       figures about the units in scope
 * @param alerts          figures about the active alerts of those units
 * @param serviceRequests figures about the owner's service requests in scope
 * @param devices         figures about the devices linked to those units
 */
public record DashboardSummary(
    Instant generatedAt,
    int siteCount,
    EquipmentFigures equipment,
    AlertFigures alerts,
    ServiceRequestFigures serviceRequests,
    DeviceFigures devices) {

  /**
   * Validates that every part of the summary is present.
   */
  public DashboardSummary {
    Objects.requireNonNull(generatedAt, "generatedAt must not be null");
    Objects.requireNonNull(equipment, "equipment must not be null");
    Objects.requireNonNull(alerts, "alerts must not be null");
    Objects.requireNonNull(serviceRequests, "serviceRequests must not be null");
    Objects.requireNonNull(devices, "devices must not be null");
  }

  /**
   * Figures about the units in scope. {@code reporting}, {@code silent} and {@code neverReported}
   * add up to {@code total}.
   *
   * @param total         units in scope
   * @param byStatus      units per lifecycle status name, every status present
   * @param reporting     units whose latest reading lies within their offline window
   * @param silent        units whose latest reading lies outside that window
   * @param neverReported units with no reading yet
   */
  public record EquipmentFigures(
      long total, Map<String, Long> byStatus, long reporting, long silent, long neverReported) {

    /**
     * Keeps the per-status totals unmodifiable and in the order they were supplied.
     */
    public EquipmentFigures {
      byStatus = Collections.unmodifiableMap(new LinkedHashMap<>(byStatus));
    }
  }

  /**
   * Figures about the alerts that are still active, that is {@code OPEN} or {@code ACKNOWLEDGED}.
   *
   * @param open             alerts nobody acknowledged yet
   * @param acknowledged     alerts acknowledged but not closed
   * @param activeBySeverity active alerts per severity, every severity present
   */
  public record AlertFigures(long open, long acknowledged, Map<AlertSeverity, Long> activeBySeverity) {

    /**
     * Keeps the per-severity totals unmodifiable and in the order they were supplied.
     */
    public AlertFigures {
      activeBySeverity = Collections.unmodifiableMap(new LinkedHashMap<>(activeBySeverity));
    }
  }

  /**
   * Figures about the owner's service requests.
   *
   * @param active   requests that are still open work
   * @param byStatus requests per status name, every status present
   */
  public record ServiceRequestFigures(long active, Map<String, Long> byStatus) {

    /**
     * Keeps the per-status totals unmodifiable and in the order they were supplied.
     */
    public ServiceRequestFigures {
      byStatus = Collections.unmodifiableMap(new LinkedHashMap<>(byStatus));
    }
  }

  /**
   * Figures about the devices linked to the units in scope.
   *
   * @param paired  devices able to report
   * @param revoked devices whose credential was withdrawn but that are still linked to a unit
   */
  public record DeviceFigures(long paired, long revoked) {
  }
}
