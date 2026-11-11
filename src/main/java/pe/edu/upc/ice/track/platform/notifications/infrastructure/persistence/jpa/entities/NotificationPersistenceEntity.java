package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.converters.MessageParamsPersistenceConverter;

import java.time.Instant;
import java.util.Map;

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

  /**
   * Key of the template the message was composed from. Nullable, so the column can be added to a
   * table that already holds rows; such a row reads back as untemplated.
   */
  @Column(name = "message_key", length = 100)
  private String messageKey;

  /** Values filled into the template, as a JSON object; nullable for the same reason. */
  @Convert(converter = MessageParamsPersistenceConverter.class)
  @Column(name = "message_params", length = 2000)
  private Map<String, Object> messageParams;

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
