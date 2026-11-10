package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.adapters;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationFilter;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationPage;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.NotificationRepository;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.assemblers.NotificationPersistenceAssembler;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.repositories.NotificationPersistenceRepository;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.specifications.NotificationPersistenceSpecifications;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Adapter fulfilling {@link NotificationRepository} on top of Spring Data JPA.
 *
 * <p>The bulk read transitions are single Criteria API {@code UPDATE} statements built by
 * {@link NotificationPersistenceSpecifications}. A bulk statement bypasses the persistence context,
 * so each one runs between a flush, so it sees pending writes, and a clear, so no managed instance
 * loaded earlier in the transaction keeps a state the statement has since overwritten.</p>
 */
@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

  /**
   * Most recent first. Identifiers are generated in insertion order, so the identifier is the
   * creation order; it is a total order too, which {@code created_at} alone is not, since
   * notifications stored before that column existed share one value.
   */
  private static final Sort INBOX_ORDER = Sort.by(Sort.Direction.DESC, "id");

  private final NotificationPersistenceRepository persistenceRepository;
  private final EntityManager entityManager;

  public NotificationRepositoryImpl(NotificationPersistenceRepository persistenceRepository, EntityManager entityManager) {
    this.persistenceRepository = persistenceRepository;
    this.entityManager = entityManager;
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
  public NotificationPage findActivePageByRecipientUserId(
      Long recipientUserId, boolean unreadOnly, NotificationFilter filter, int page, int size) {
    if (recipientUserId == null) {
      return NotificationPage.empty(page, size);
    }
    var specification = NotificationPersistenceSpecifications.inbox(recipientUserId, unreadOnly, filter);
    // An offset beyond Integer.MAX_VALUE cannot be expressed; such a page is necessarily empty.
    if ((long) page * size > Integer.MAX_VALUE) {
      return new NotificationPage(List.of(), page, size, persistenceRepository.count(specification));
    }
    var slice = persistenceRepository.findAll(specification, PageRequest.of(page, size, INBOX_ORDER));
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

  @Override
  @Transactional
  public long markActiveUnreadAsRead(Long recipientUserId, NotificationFilter filter, Instant readAt) {
    if (recipientUserId == null || readAt == null) {
      return 0L;
    }
    var update = NotificationPersistenceSpecifications.markActiveUnreadAsRead(recipientUserId, filter, readAt);
    return bulk(() -> persistenceRepository.update(update));
  }

  @Override
  @Transactional
  public long markActiveUnreadAsReadByIds(Long recipientUserId, Collection<Long> notificationIds, Instant readAt) {
    if (recipientUserId == null || readAt == null || notificationIds == null || notificationIds.isEmpty()) {
      return 0L;
    }
    var update = NotificationPersistenceSpecifications.markActiveUnreadAsReadByIds(
        recipientUserId, notificationIds, readAt);
    return bulk(() -> persistenceRepository.update(update));
  }

  /**
   * Runs a bulk statement between a flush and a clear of the persistence context.
   *
   * @param statement the bulk statement, returning the number of affected rows
   * @return the number of affected rows
   */
  private long bulk(LongSupplier statement) {
    entityManager.flush();
    try {
      return statement.getAsLong();
    } finally {
      entityManager.clear();
    }
  }
}
