package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestFilter;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.ServiceRequestPersistenceEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Criteria API specifications over {@link ServiceRequestPersistenceEntity}.
 *
 * <p>Every listing is scoped first - to the owner who created the requests or to the technician
 * they are assigned to - and each filter, when one is supplied, is {@code AND}-ed to that scope, so
 * it can only ever narrow the caller's own requests. As in the equipment listing, the
 * {@code WHERE} clause only carries the predicates that restrict something.</p>
 */
public final class ServiceRequestPersistenceSpecifications {

  private static final String OWNER_ID = "ownerId";
  private static final String TECHNICIAN_ID = "technicianId";
  private static final String STATUS = "status";
  private static final String EQUIPMENT_ID = "equipmentId";
  private static final String SITE_ID = "siteId";
  private static final String TYPE = "type";
  private static final String PRIORITY = "priority";
  private static final String REQUESTED_AT = "requestedAt";
  private static final String SOURCE_ALERT_ID = "sourceAlertId";

  private ServiceRequestPersistenceSpecifications() {
  }

  /**
   * Builds the specification behind an owner's listing.
   *
   * @param ownerId the owner; required
   * @param filter  the restrictions; {@code null} restricts nothing
   * @return the specification, to be used for both the rows and their count
   */
  public static Specification<ServiceRequestPersistenceEntity> ownedBy(Long ownerId, ServiceRequestFilter filter) {
    return scopedListing(OWNER_ID, Objects.requireNonNull(ownerId, "ownerId must not be null"), filter);
  }

  /**
   * Builds the specification behind a technician's listing of its assignments.
   *
   * @param technicianId the technician; required
   * @param filter       the restrictions; {@code null} restricts nothing
   * @return the specification, to be used for both the rows and their count
   */
  public static Specification<ServiceRequestPersistenceEntity> assignedTo(
      Long technicianId, ServiceRequestFilter filter) {
    return scopedListing(
        TECHNICIAN_ID, Objects.requireNonNull(technicianId, "technicianId must not be null"), filter);
  }

  private static Specification<ServiceRequestPersistenceEntity> scopedListing(
      String scopeAttribute, Long scopeValue, ServiceRequestFilter filter) {
    var normalized = filter == null ? ServiceRequestFilter.NONE : filter;
    return (root, query, criteriaBuilder) -> {
      var predicates = new ArrayList<Predicate>();
      predicates.add(criteriaBuilder.equal(root.get(scopeAttribute), scopeValue));
      if (!normalized.statuses().isEmpty()) {
        predicates.add(root.get(STATUS).in(normalized.statuses()));
      }
      if (normalized.equipmentId() != null) {
        predicates.add(criteriaBuilder.equal(root.get(EQUIPMENT_ID), normalized.equipmentId()));
      }
      if (normalized.siteId() != null) {
        predicates.add(criteriaBuilder.equal(root.get(SITE_ID), normalized.siteId()));
      }
      if (!normalized.types().isEmpty()) {
        predicates.add(root.get(TYPE).in(normalized.types()));
      }
      if (!normalized.priorities().isEmpty()) {
        predicates.add(root.get(PRIORITY).in(normalized.priorities()));
      }
      if (normalized.createdFrom() != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<Instant>get(REQUESTED_AT), normalized.createdFrom()));
      }
      if (normalized.createdTo() != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<Instant>get(REQUESTED_AT), normalized.createdTo()));
      }
      if (normalized.sourceAlertId() != null) {
        predicates.add(criteriaBuilder.equal(root.get(SOURCE_ALERT_ID), normalized.sourceAlertId()));
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }
}
