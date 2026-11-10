package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.AlertPersistenceEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Criteria API specifications over {@link AlertPersistenceEntity}.
 *
 * <p>The owner-wide listing is scoped first - to the units the caller owns - and every optional
 * filter is {@code AND}-ed to that scope, so a filter can only ever narrow the caller's own alerts.
 * The {@code WHERE} clause only carries the predicates that restrict something. Status, type and
 * severity are stored by the name of their constant, so they are matched by name.</p>
 */
public final class AlertPersistenceSpecifications {

  private static final String EQUIPMENT_ID = "equipmentId";
  private static final String STATUS = "status";
  private static final String TYPE = "type";
  private static final String SEVERITY = "severity";
  private static final String OPENED_AT = "openedAt";

  private AlertPersistenceSpecifications() {
  }

  /**
   * Builds the specification behind an owner's alert listing.
   *
   * @param equipmentIds identifiers of the units that scope the listing; required and not empty
   * @param statuses     restrict to these statuses; {@code null} or empty does not restrict
   * @param types        restrict to these kinds of alert; {@code null} or empty does not restrict
   * @param severities   restrict to these severities; {@code null} or empty does not restrict
   * @param openedFrom   inclusive lower bound of the opening instant, or {@code null}
   * @param openedTo     inclusive upper bound of the opening instant, or {@code null}
   * @return the specification, to be used for both the rows and their count
   */
  public static Specification<AlertPersistenceEntity> raisedOnEquipment(
      Collection<Long> equipmentIds,
      Collection<? extends Enum<?>> statuses,
      Collection<? extends Enum<?>> types,
      Collection<? extends Enum<?>> severities,
      Instant openedFrom,
      Instant openedTo) {
    Objects.requireNonNull(equipmentIds, "equipmentIds must not be null");
    var scope = Set.copyOf(equipmentIds);
    var statusNames = namesOf(statuses);
    var typeNames = namesOf(types);
    var severityNames = namesOf(severities);
    return (root, query, criteriaBuilder) -> {
      var predicates = new ArrayList<Predicate>();
      predicates.add(root.get(EQUIPMENT_ID).in(scope));
      if (statusNames != null) {
        predicates.add(root.get(STATUS).in(statusNames));
      }
      if (typeNames != null) {
        predicates.add(root.get(TYPE).in(typeNames));
      }
      if (severityNames != null) {
        predicates.add(root.get(SEVERITY).in(severityNames));
      }
      if (openedFrom != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<Instant>get(OPENED_AT), openedFrom));
      }
      if (openedTo != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<Instant>get(OPENED_AT), openedTo));
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  /**
   * Turns a filter of enum constants into the names they are stored by.
   *
   * @param constants the constants, may be {@code null}
   * @return their names, or {@code null} when the filter does not restrict
   */
  private static Set<String> namesOf(Collection<? extends Enum<?>> constants) {
    if (constants == null || constants.isEmpty()) {
      return null;
    }
    return constants.stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());
  }
}
