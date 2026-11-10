package pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportListFilter;
import pe.edu.upc.ice.track.platform.reporting.infrastructure.persistence.jpa.entities.ReportPersistenceEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/**
 * Criteria API specifications over {@link ReportPersistenceEntity}.
 *
 * <p>The listing is scoped to the requesting user first, and every filter is {@code AND}-ed to that
 * scope, so it can only ever narrow the caller's own history. The {@code WHERE} clause only carries
 * the predicates that restrict something.</p>
 */
public final class ReportPersistenceSpecifications {

  private static final String USER_ID = "userId";
  private static final String TITLE = "title";
  private static final String TYPE = "type";
  private static final String STATUS = "status";
  private static final String REQUESTED_AT = "requestedAt";
  private static final char LIKE_ESCAPE = '\\';

  private ReportPersistenceSpecifications() {
  }

  /**
   * Builds the specification behind a user's report listing.
   *
   * <p>The search text matches anywhere in the report name, ignoring case; {@code %} and {@code _}
   * in it are matched literally rather than as wildcards.</p>
   *
   * @param userId the requesting user; required
   * @param filter name, type, status and request-date restrictions; {@code null} restricts nothing
   * @return the specification, to be used for both the rows and their count
   */
  public static Specification<ReportPersistenceEntity> requestedBy(Long userId, ReportListFilter filter) {
    Objects.requireNonNull(userId, "userId must not be null");
    var normalized = filter == null ? ReportListFilter.NONE : filter;
    var pattern = normalized.search() == null ? null : "%" + escapeLike(normalized.search().toLowerCase(Locale.ROOT)) + "%";
    return (root, query, criteriaBuilder) -> {
      var predicates = new ArrayList<Predicate>();
      predicates.add(criteriaBuilder.equal(root.get(USER_ID), userId));
      if (pattern != null) {
        predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(TITLE)), pattern, LIKE_ESCAPE));
      }
      if (!normalized.types().isEmpty()) {
        predicates.add(root.get(TYPE).in(normalized.types()));
      }
      if (!normalized.statuses().isEmpty()) {
        predicates.add(root.get(STATUS).in(normalized.statuses()));
      }
      if (normalized.requestedFrom() != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<Instant>get(REQUESTED_AT), normalized.requestedFrom()));
      }
      if (normalized.requestedTo() != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<Instant>get(REQUESTED_AT), normalized.requestedTo()));
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static String escapeLike(String text) {
    var escaped = new StringBuilder(text.length());
    for (var character : text.toCharArray()) {
      if (character == '%' || character == '_' || character == LIKE_ESCAPE) {
        escaped.append(LIKE_ESCAPE);
      }
      escaped.append(character);
    }
    return escaped.toString();
  }
}
