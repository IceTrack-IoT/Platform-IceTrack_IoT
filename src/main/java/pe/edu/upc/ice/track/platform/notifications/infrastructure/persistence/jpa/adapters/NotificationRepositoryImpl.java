package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.adapters;

import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.NotificationRepository;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.assemblers.NotificationPersistenceAssembler;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.repositories.NotificationPersistenceRepository;

import java.util.List;
import java.util.Optional;

/** Adapter fulfilling {@link NotificationRepository} on top of Spring Data JPA. */
@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

  private final NotificationPersistenceRepository persistenceRepository;

  public NotificationRepositoryImpl(NotificationPersistenceRepository persistenceRepository) {
    this.persistenceRepository = persistenceRepository;
  }

  @Override
  public Notification save(Notification notification) {
    var entity = NotificationPersistenceAssembler.toPersistenceEntityFromDomain(notification);
    var saved = persistenceRepository.save(entity);
    return NotificationPersistenceAssembler.toDomainFromPersistenceEntity(saved);
  }

  @Override
  public Optional<Notification> findById(Long notificationId) {
    return persistenceRepository.findById(notificationId)
        .map(NotificationPersistenceAssembler::toDomainFromPersistenceEntity);
  }

  @Override
  public List<Notification> findByRecipientUserId(Long recipientUserId) {
    return persistenceRepository.findByRecipientUserId(recipientUserId).stream()
        .map(NotificationPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
  }

  @Override
  public List<Notification> findByRecipientUserIdAndIsReadFalse(Long recipientUserId) {
    return persistenceRepository.findByRecipientUserIdAndIsReadFalse(recipientUserId).stream()
        .map(NotificationPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
  }
}
