package pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.Temperature;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

/**
 * AlertPolicy aggregate root of the {@code monitoring} bounded context.
 *
 * <p>A policy with {@code equipmentId == null} is the platform-wide default, evaluated whenever
 * no equipment-specific policy is active for the equipment being evaluated. Kept as its own
 * aggregate — rather than embedded on the Asset Management {@code Equipment} — because it is
 * queried and updated independently of the equipment catalogue, and belongs to the context that
 * actually consumes it when evaluating a reading.</p>
 *
 * <p>{@code hysteresisMargin} is a {@link Temperature} Value Object, not a raw {@code Double} —
 * it is a Celsius value just like {@code SensorReading.maxTemperature}, so it earns the same
 * physical-plausibility guarantee. {@code sustainedExcursionMinutes} and
 * {@code missedSyncWindowsForOffline} stay as plain {@code Integer}: they are counts of minutes
 * and sync windows, not measured physical quantities, so there is no domain rule to enforce
 * beyond "non-negative", which the REST layer already validates at the boundary.</p>
 */
@Getter
public class AlertPolicy extends AbstractDomainAggregateRoot<AlertPolicy> {

  private Long id;
  private EquipmentId equipmentId;
  private Integer sustainedExcursionMinutes;
  private Temperature hysteresisMargin;
  private Integer missedSyncWindowsForOffline;
  private boolean active;

  public AlertPolicy(EquipmentId equipmentId, Integer sustainedExcursionMinutes,
                     Double hysteresisMarginCelsius, Integer missedSyncWindowsForOffline) {
    this.equipmentId = equipmentId;
    this.sustainedExcursionMinutes = sustainedExcursionMinutes;
    this.hysteresisMargin = new Temperature(hysteresisMarginCelsius);
    this.missedSyncWindowsForOffline = missedSyncWindowsForOffline;
    this.active = true;
  }

  public void assignId(Long id) {
    this.id = id;
  }

  public void updatePolicy(Integer sustainedExcursionMinutes, Double hysteresisMarginCelsius,
                            Integer missedSyncWindowsForOffline) {
    this.sustainedExcursionMinutes = sustainedExcursionMinutes;
    this.hysteresisMargin = new Temperature(hysteresisMarginCelsius);
    this.missedSyncWindowsForOffline = missedSyncWindowsForOffline;
  }

  public void deactivate() {
    this.active = false;
  }
}
