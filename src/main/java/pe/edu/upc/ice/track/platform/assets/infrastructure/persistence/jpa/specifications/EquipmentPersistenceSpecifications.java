package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.entities.EquipmentPersistenceEntity;
import pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.entities.SitePersistenceEntity;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/**
 * Criteria API specifications over {@link EquipmentPersistenceEntity}.
 *
 * <p>The listing filters are optional and combine freely, so the {@code WHERE} clause is assembled
 * from the filters actually supplied instead of being written once with a
 * {@code (:p IS NULL OR ...)} guard per filter: the statement the database plans only carries the
 * predicates that restrict something, and no {@code null} is ever bound to a parameter whose type
 * the driver would have to guess.</p>
 *
 * <p>Every value reaches the database as a bound parameter, never concatenated into the
 * statement. The search text is additionally escaped for {@code LIKE}, so a {@code %} or an
 * {@code _} typed by the user is matched literally instead of acting as a wildcard.</p>
 */
public final class EquipmentPersistenceSpecifications {

  /** Escape character declared on every {@code LIKE} built here. */
  private static final char LIKE_ESCAPE = '\\';

  private static final String SITE_ID = "siteId";
  private static final String STATUS = "status";
  private static final String EQUIPMENT_TYPE = "equipmentType";
  private static final String NAME = "name";
  private static final String EQUIPMENT_UID = "equipmentUid";
  private static final String SITE_IDENTIFIER = "id";
  private static final String SITE_OWNER_PROFILES_ID = "ownerProfilesId";

  private EquipmentPersistenceSpecifications() {
  }

  /**
   * Builds the specification behind an owner's equipment listing.
   *
   * <p>The ownership rule is always the first predicate and every optional filter is
   * {@code AND}-ed to it, the search included: its two alternatives are grouped in their own
   * {@code OR}, so a match on the name or the uid can only ever narrow the owner's units, never add
   * another owner's. A unit carries no owner column of its own, so ownership is expressed as a
   * subquery on the owner's sites: {@code sites} is probed through its owner index and the
   * equipment table is then filtered on {@code site_id}.</p>
   *
   * @param ownerProfilesId the owner's profile identifier; required
   * @param siteId          restrict to a single site, or {@code null}
   * @param status          restrict to a single status, or {@code null}
   * @param equipmentType   restrict to a single type, or {@code null}
   * @param search          case-insensitive text matched against the name or the uid, or
   *                        {@code null}; a blank value does not restrict
   * @return the specification, to be used for both the rows and their count
   */
  public static Specification<EquipmentPersistenceEntity> ownerListing(
      Long ownerProfilesId, Long siteId, StatusEquipment status, EquipmentType equipmentType, String search) {
    Objects.requireNonNull(ownerProfilesId, "ownerProfilesId must not be null");
    var searchPattern = toContainsPattern(search);
    return (root, query, criteriaBuilder) -> {
      var predicates = new ArrayList<Predicate>();

      var ownedSiteIds = Objects.requireNonNull(query, "query must not be null").subquery(Long.class);
      var site = ownedSiteIds.from(SitePersistenceEntity.class);
      ownedSiteIds
          .select(site.<Long>get(SITE_IDENTIFIER))
          .where(criteriaBuilder.equal(site.get(SITE_OWNER_PROFILES_ID), ownerProfilesId));
      predicates.add(root.get(SITE_ID).in(ownedSiteIds));

      if (siteId != null) {
        predicates.add(criteriaBuilder.equal(root.get(SITE_ID), siteId));
      }
      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get(STATUS), status));
      }
      if (equipmentType != null) {
        predicates.add(criteriaBuilder.equal(root.get(EQUIPMENT_TYPE), equipmentType));
      }
      if (searchPattern != null) {
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.<String>get(NAME)), searchPattern, LIKE_ESCAPE),
            criteriaBuilder.like(criteriaBuilder.lower(root.<String>get(EQUIPMENT_UID)), searchPattern, LIKE_ESCAPE)));
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
    return "%" + escapeLikeWildcards(search.strip().toLowerCase(Locale.ROOT)) + "%";
  }

  /**
   * Escapes the characters {@code LIKE} would otherwise interpret. The escape character itself is
   * escaped first, so the escapes added for the wildcards are not escaped again.
   *
   * @param text the text to escape
   * @return the text with {@code \}, {@code %} and {@code _} escaped by {@link #LIKE_ESCAPE}
   */
  private static String escapeLikeWildcards(String text) {
    return text
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
  }
}
