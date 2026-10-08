package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.LocalDateTime;

/**
 * JPA persistence entity for an {@code Equipment}, mapped to the {@code assets.equipments} table.
 *
 * <p>Kept separate from the {@code Equipment} aggregate, exactly like {@link
 * SitePersistenceEntity} is kept separate from {@code Site}.</p>
 *
 * <p>Three index groups, each matching a query this context actually serves:</p>
 * <ul>
 *   <li>{@code equipment_uid} is unique, because a uid printed on a device is what Device
 *       Management resolves a unit by and two units answering to the same uid would make that
 *       resolution ambiguous;</li>
 *   <li>{@code site_id} serves the "equipment of a site" listing;</li>
 *   <li>{@code (status, equipment_type)} serves the US-11 dashboard filters, which always narrow
 *       by those two columns and are read far more often than they are written.</li>
 * </ul>
 *
 * <p>Both enums are stored as {@code STRING}, not as an ordinal: an ordinal silently rewrites
 * itself whenever a constant is inserted, which would turn a routine enum change into corrupted
 * rows.</p>
 *
 * <p>{@code siteId} is a plain column rather than a {@code @ManyToOne}. There is no
 * {@code Site} entity to point at - sites and equipments are two independent aggregates reached
 * through their identifiers, and joining is the repository's job.</p>
 */
@Getter
@Setter
@Entity
@Table(
    name = "equipments",
    schema = "assets",
    indexes = {
        @Index(name = "idx_equipments_uid", columnList = "equipment_uid", unique = true),
        @Index(name = "idx_equipments_site_id", columnList = "site_id"),
        @Index(name = "idx_equipments_status_type", columnList = "status, equipment_type")})
@Check(constraints = "threshold_min_celsius < threshold_max_celsius AND reminder_interval_days > 0")
public class EquipmentPersistenceEntity extends AuditableAbstractPersistenceEntity {

  /**
   * Identifier of the site this unit is installed at. A plain column, not an association: the two
   * aggregates are joined by identifier, and the owner of a unit is reached through this column.
   */
  @Column(name = "site_id", nullable = false)
  private Long siteId;

  /** The unit's own identifier, printed on the device and unique across the platform. */
  @Column(name = "equipment_uid", nullable = false, length = 30)
  private String equipmentUid;

  @Column(name = "name", nullable = false, length = 30)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private StatusEquipment status;

  @Enumerated(EnumType.STRING)
  @Column(name = "equipment_type", nullable = false, length = 35)
  private EquipmentType equipmentType;

  @Column(name = "online", nullable = false)
  private boolean online;

  /**
   * Preventive maintenance interval in days. The {@code > 0} rule is enforced by the domain and
   * re-asserted by a {@code CHECK} constraint on the table.
   */
  @Column(name = "reminder_interval_days", nullable = false)
  private Integer reminderIntervalDays;

  /**
   * Lowest acceptable temperature in Celsius. Kept as its own column, next to its maximum, so the
   * database can enforce {@code min < max} and so a band stays queryable by Monitoring.
   */
  @Column(name = "threshold_min_celsius", nullable = false)
  private Double thresholdMinCelsius;

  /**
   * Highest acceptable temperature in Celsius, re-asserted against
   * {@link #thresholdMinCelsius} by a {@code CHECK} constraint on the table.
   */
  @Column(name = "threshold_max_celsius", nullable = false)
  private Double thresholdMaxCelsius;

  /**
   * When the last reading was received. Written by the Monitoring and Alerting context, read and
   * exposed here; nullable because a unit may not have reported yet.
   */
  @Column(name = "last_reading_at")
  private LocalDateTime lastReadingAt;

  /**
   * Last temperature known for this unit, in Celsius. Written by the Monitoring and Alerting
   * context, read and exposed here; nullable because a unit may not have reported yet.
   */
  @Column(name = "last_known_temperature")
  private Double lastKnownTemperature;

  public EquipmentPersistenceEntity() {
  }
}