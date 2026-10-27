package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.DevicePlatform;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_tokens")
@Getter
@Setter
@NoArgsConstructor
public class DeviceTokenPersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long recipientUserId;

  /** FCM tokens are up to ~4 KB in the worst case; 512 covers every token issued today. */
  @Column(nullable = false, unique = true, length = 512)
  private String token;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private DevicePlatform platform;

  @Column(nullable = false)
  private LocalDateTime lastSeenAt;
}
