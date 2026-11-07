package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.MaintenanceWindowPersistenceEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Criteria API specifications over {@link MaintenanceWindowPersistenceEntity}.
 *
 * <p>Both lookups combine an optional or alternative bound that a derived query name cannot express
 * without repeating every predicate on each side of an {@code OR}.</p>
 */
public final class MaintenanceWindowPersistenceSpecifications {

  private static final String EQUIPMENT_ID = "equipmentId";
  private static final String STARTED_AT = "startedAt";
  private static final String ENDED_AT = "endedAt";

  private MaintenanceWindowPersistenceSpecifications() {
  }

  /**
   * Windows of a unit covering an instant: started at or before it, and either still active or
   * ended strictly after it.
   *
   * @param equipmentId the unit; required
   * @param instant     the instant; required
   * @return the specification
   */
  public static Specification<MaintenanceWindowPersistenceEntity> covering(Long equipmentId, Instant instant) {
    Objects.requireNonNull(equipmentId, "equipmentId must not be null");
    Objects.requireNonNull(instant, "instant must not be null");
    return (root, query, criteriaBuilder) -> criteriaBuilder.and(
        criteriaBuilder.equal(root.get(EQUIPMENT_ID), equipmentId),
        criteriaBuilder.lessThanOrEqualTo(root.get(STARTED_AT), instant),
        criteriaBuilder.or(
            criteriaBuilder.isNull(root.get(ENDED_AT)),
            criteriaBuilder.greaterThan(root.get(ENDED_AT), instant)));
  }

  /**
   * Windows of a unit started within a period whose bounds are both optional.
   *
   * @param equipmentId the unit; required
   * @param from        inclusive lower bound of the start, or {@code null}
   * @param to          inclusive upper bound of the start, or {@code null}
   * @return the specification
   */
  public static Specification<MaintenanceWindowPersistenceEntity> startedBetween(
      Long equipmentId, Instant from, Instant to) {
    Objects.requireNonNull(equipmentId, "equipmentId must not be null");
    return (root, query, criteriaBuilder) -> {
      var predicates = new ArrayList<Predicate>();
      predicates.add(criteriaBuilder.equal(root.get(EQUIPMENT_ID), equipmentId));
      if (from != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(STARTED_AT), from));
      }
      if (to != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(STARTED_AT), to));
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }
}
