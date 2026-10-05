package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

  @Column(nullable = false, length = 255)
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private String type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private String severity;

  @Column(nullable = false)
  private boolean isRead;

  private LocalDateTime readAt;
  private LocalDateTime dismissedAt;

}
