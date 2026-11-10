package pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.device.infrastructure.persistence.jpa.entities.DevicePersistenceEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Criteria API specifications over {@link DevicePersistenceEntity}.
 *
 * <p>The inventory listing is scoped first - to the units the caller owns - and every optional
 * filter is {@code AND}-ed to that scope, so it can only ever narrow the caller's own boards. The
 * {@code WHERE} clause only carries the predicates that restrict something, and the search text is
 * escaped for {@code LIKE}, so a {@code %} or an {@code _} typed by the user is matched literally.</p>
 */
public final class DevicePersistenceSpecifications {

  /** Escape character declared on every {@code LIKE} built here. */
  private static final char LIKE_ESCAPE = '\\';

  private static final String PAIRED_EQUIPMENT_ID = "pairedEquipmentId";
  private static final String STATUS = "status";
  private static final String DEVICE_UID = "deviceUid";
  private static final String BOARD_MODEL = "boardModel";

  private DevicePersistenceSpecifications() {
  }

  /**
   * Builds the specification behind an owner's device inventory.
   *
   * <p>An unpaired board holds no unit, so it never satisfies the scope: it cannot appear whatever
   * the status filter asks for.</p>
   *
   * @param equipmentIds identifiers of the units that scope the listing; required and not empty
   * @param statuses     restrict to these statuses; {@code null} or empty does not restrict
   * @param search       case-insensitive text matched against the board uid or model, or
   *                     {@code null}; a blank value does not restrict
   * @return the specification, to be used for both the rows and their count
   */
  public static Specification<DevicePersistenceEntity> linkedToEquipment(
      Collection<Long> equipmentIds, Collection<DeviceStatus> statuses, String search) {
    Objects.requireNonNull(equipmentIds, "equipmentIds must not be null");
    var scope = Set.copyOf(equipmentIds);
    var statusFilter = statuses == null || statuses.isEmpty() ? null : Set.copyOf(statuses);
    var searchPattern = toContainsPattern(search);
    return (root, query, criteriaBuilder) -> {
      var predicates = new ArrayList<Predicate>();
      predicates.add(root.get(PAIRED_EQUIPMENT_ID).in(scope));
      if (statusFilter != null) {
        predicates.add(root.get(STATUS).in(statusFilter));
      }
      if (searchPattern != null) {
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.<String>get(DEVICE_UID)), searchPattern, LIKE_ESCAPE),
            criteriaBuilder.like(criteriaBuilder.lower(root.<String>get(BOARD_MODEL)), searchPattern, LIKE_ESCAPE)));
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  /**
   * Turns free text into a lower-case "contains" pattern for {@code LIKE}.
   *
   * @param search the text typed by the user, may be {@code null}
   * @return the pattern, or {@code null} when there is nothing to search for
   */
  private static String toContainsPattern(String search) {
    if (search == null || search.isBlank()) {
      return null;
    }
    var escaped = search.strip().toLowerCase(Locale.ROOT)
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
    return "%" + escaped + "%";
  }
}
