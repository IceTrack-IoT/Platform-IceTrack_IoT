package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;

import java.time.Instant;

/**
 * JPA persistence entity for a {@code Notification}, mapped to the {@code notifications} table.
 *
 * <p>{@code equipmentId}, {@code deviceId}, {@code sourceAlertId} and {@code serviceRequestId} are
 * plain columns, never foreign keys: what they point at lives in other bounded contexts.</p>
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long recipientUserId;

  private Long equipmentId;
  private Long deviceId;
  private Long sourceAlertId;

  /** The service request the notification is about; {@code NULL} when it concerns none. */
  private Long serviceRequestId;

  @Column(nullable = false, length = 255)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private NotificationType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private NotificationSeverity severity;

  @Column(nullable = false)
  private boolean isRead;

  /**
   * When the notification was raised. Written on insert only. The column default lets the schema
   * update add it to a table that already holds rows: a notification stored before the column
   * existed carries the moment the column was added, the closest value still known for it.
   */
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  private Instant readAt;
  private Instant dismissedAt;

}
