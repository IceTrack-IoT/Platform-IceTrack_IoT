package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.adapters;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationPage;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.NotificationRepository;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.assemblers.NotificationPersistenceAssembler;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.repositories.NotificationPersistenceRepository;

import java.util.List;
import java.util.Optional;

/** Adapter fulfilling {@link NotificationRepository} on top of Spring Data JPA. */
@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

  /**
   * Most recent first. Notifications carry no creation timestamp, and identifiers are generated in
   * insertion order, so the identifier is the creation order.
   */
  private static final Sort INBOX_ORDER = Sort.by(Sort.Direction.DESC, "id");

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
  public NotificationPage findActivePageByRecipientUserId(Long recipientUserId, boolean unreadOnly, int page, int size) {
    if (recipientUserId == null) {
      return NotificationPage.empty(page, size);
    }
    // An offset beyond Integer.MAX_VALUE cannot be expressed; such a page is necessarily empty.
    if ((long) page * size > Integer.MAX_VALUE) {
      return new NotificationPage(List.of(), page, size, countActive(recipientUserId, unreadOnly));
    }
    var pageable = PageRequest.of(page, size, INBOX_ORDER);
    var slice = unreadOnly
        ? persistenceRepository.findByRecipientUserIdAndIsReadFalseAndDismissedAtIsNull(recipientUserId, pageable)
        : persistenceRepository.findByRecipientUserIdAndDismissedAtIsNull(recipientUserId, pageable);
    var content = slice.getContent().stream()
        .map(NotificationPersistenceAssembler::toDomainFromPersistenceEntity)
        .toList();
    return new NotificationPage(content, page, size, slice.getTotalElements());
  }

  @Override
  public long countActiveUnreadByRecipientUserId(Long recipientUserId) {
    return recipientUserId == null
        ? 0L
        : persistenceRepository.countByRecipientUserIdAndIsReadFalseAndDismissedAtIsNull(recipientUserId);
  }

  private long countActive(Long recipientUserId, boolean unreadOnly) {
    var firstRow = PageRequest.of(0, 1, INBOX_ORDER);
    return unreadOnly
        ? persistenceRepository.findByRecipientUserIdAndIsReadFalseAndDismissedAtIsNull(recipientUserId, firstRow).getTotalElements()
        : persistenceRepository.findByRecipientUserIdAndDismissedAtIsNull(recipientUserId, firstRow).getTotalElements();
  }
}
