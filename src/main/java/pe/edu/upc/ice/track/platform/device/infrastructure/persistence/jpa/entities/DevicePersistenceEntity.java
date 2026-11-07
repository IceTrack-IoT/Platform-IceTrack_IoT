package pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.LocalDateTime;

/**
 * JPA representation of the Device aggregate.
 *
 * <p>{@code api_key_digest} carries a unique index: it is the column every edge request is
 * authenticated by. Unpaired and revoked boards hold {@code NULL} there, which a unique index
 * accepts any number of times.</p>
 */
@Getter
@Setter
@Entity
@Table(
    name = "devices",
    indexes = {
        @Index(name = "idx_devices_uid", columnList = "device_uid", unique = true),
        @Index(name = "idx_devices_api_key_digest", columnList = "api_key_digest", unique = true),
        @Index(name = "idx_devices_equipment_id", columnList = "paired_equipment_id"),
        @Index(name = "idx_devices_status", columnList = "status")})
public class DevicePersistenceEntity extends AuditableAbstractPersistenceEntity {

  /** Identifier physically assigned to the ESP32 board, unique across the fleet. */
  @Column(name = "device_uid", nullable = false, length = 64)
  private String deviceUid;

  @Column(name = "board_model", nullable = false, length = 64)
  private String boardModel;

  @Column(name = "firmware_version", length = 64)
  private String firmwareVersion;

  /** Hex SHA-256 digest of the API key; the plaintext key never appears in this entity. */
  @Column(name = "api_key_digest", length = 64)
  private String apiKeyDigest;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private DeviceStatus status;

  /** Plain identifier rather than an ORM association, because Equipment belongs to another aggregate. */
  @Column(name = "paired_equipment_id")
  private Long pairedEquipmentId;

  @Column(name = "last_seen_at")
  private LocalDateTime lastSeenAt;

  public DevicePersistenceEntity() {
  }
}
