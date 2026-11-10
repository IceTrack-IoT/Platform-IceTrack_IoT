package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.specifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.UpdateSpecification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationFilter;
import pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.entities.NotificationPersistenceEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Criteria API specifications over {@link NotificationPersistenceEntity}: the inbox listing and the
 * bulk read transitions.
 *
 * <p>Every specification starts from the recipient and the active ({@code dismissed_at IS NULL})
 * scope, and each filter is {@code AND}-ed to it, so a filter can only ever narrow the recipient's
 * own notifications. The {@code WHERE} clause only carries the predicates that restrict
 * something.</p>
 *
 * <p>The bulk read updates are conditional on {@code is_read = false}, so each one is an atomic
 * compare-and-set: run twice, or concurrently with a single read, a notification changes once and
 * is counted once.</p>
 */
public final class NotificationPersistenceSpecifications {

  private static final String ID = "id";
  private static final String RECIPIENT_USER_ID = "recipientUserId";
  private static final String EQUIPMENT_ID = "equipmentId";
  private static final String TYPE = "type";
  private static final String SEVERITY = "severity";
  private static final String IS_READ = "isRead";
  private static final String READ_AT = "readAt";
  private static final String DISMISSED_AT = "dismissedAt";

  private NotificationPersistenceSpecifications() {
  }

  /**
   * Builds the specification behind a recipient's inbox page.
   *
   * @param recipientUserId the recipient; required
   * @param unreadOnly      {@code true} to keep only notifications not yet read
   * @param filter          severity, type and unit restrictions; {@code null} restricts nothing
   * @return the specification, to be used for both the rows and their count
   */
  public static Specification<NotificationPersistenceEntity> inbox(
      Long recipientUserId, boolean unreadOnly, NotificationFilter filter) {
    Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
    var normalized = filter == null ? NotificationFilter.NONE : filter;
    return (root, query, criteriaBuilder) -> {
      var predicates = activeOf(root, criteriaBuilder, recipientUserId);
      if (unreadOnly) {
        predicates.add(criteriaBuilder.isFalse(root.<Boolean>get(IS_READ)));
      }
      addFilter(predicates, root, criteriaBuilder, normalized);
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  /**
   * Marks as read every active and unread notification of a recipient that matches a filter.
   *
   * @param recipientUserId the recipient; required
   * @param filter          severity, type and unit restrictions; {@code null} restricts nothing
   * @param readAt          the read instant to record; required
   * @return the update
   */
  public static UpdateSpecification<NotificationPersistenceEntity> markActiveUnreadAsRead(
      Long recipientUserId, NotificationFilter filter, Instant readAt) {
    Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
    Objects.requireNonNull(readAt, "readAt must not be null");
    var normalized = filter == null ? NotificationFilter.NONE : filter;
    return (root, update, criteriaBuilder) -> {
      update.set(root.<Boolean>get(IS_READ), Boolean.TRUE);
      update.set(root.<Instant>get(READ_AT), readAt);
      var predicates = activeOf(root, criteriaBuilder, recipientUserId);
      predicates.add(criteriaBuilder.isFalse(root.<Boolean>get(IS_READ)));
      addFilter(predicates, root, criteriaBuilder, normalized);
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  /**
   * Marks as read the listed notifications that belong to a recipient and are active and unread.
   *
   * @param recipientUserId the recipient; required
   * @param notificationIds the notifications to mark; required and not empty
   * @param readAt          the read instant to record; required
   * @return the update
   */
  public static UpdateSpecification<NotificationPersistenceEntity> markActiveUnreadAsReadByIds(
      Long recipientUserId, Collection<Long> notificationIds, Instant readAt) {
    Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
    Objects.requireNonNull(readAt, "readAt must not be null");
    if (notificationIds == null || notificationIds.isEmpty()) {
      throw new IllegalArgumentException("notificationIds must not be empty");
    }
    var ids = Set.copyOf(notificationIds);
    return (root, update, criteriaBuilder) -> {
      update.set(root.<Boolean>get(IS_READ), Boolean.TRUE);
      update.set(root.<Instant>get(READ_AT), readAt);
      var predicates = activeOf(root, criteriaBuilder, recipientUserId);
      predicates.add(criteriaBuilder.isFalse(root.<Boolean>get(IS_READ)));
      predicates.add(root.get(ID).in(ids));
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static List<Predicate> activeOf(
      Root<NotificationPersistenceEntity> root, CriteriaBuilder criteriaBuilder, Long recipientUserId) {
    var predicates = new ArrayList<Predicate>();
    predicates.add(criteriaBuilder.equal(root.get(RECIPIENT_USER_ID), recipientUserId));
    predicates.add(criteriaBuilder.isNull(root.get(DISMISSED_AT)));
    return predicates;
  }

  private static void addFilter(List<Predicate> predicates, Root<NotificationPersistenceEntity> root,
                                CriteriaBuilder criteriaBuilder, NotificationFilter filter) {
    if (!filter.severities().isEmpty()) {
      predicates.add(root.get(SEVERITY).in(filter.severities()));
    }
    if (!filter.types().isEmpty()) {
      predicates.add(root.get(TYPE).in(filter.types()));
    }
    if (filter.equipmentId() != null) {
      predicates.add(criteriaBuilder.equal(root.get(EQUIPMENT_ID), filter.equipmentId()));
    }
  }
}
