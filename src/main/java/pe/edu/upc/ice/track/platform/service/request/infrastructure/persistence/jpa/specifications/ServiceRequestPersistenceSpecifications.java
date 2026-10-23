package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.specifications;

import org.springframework.data.jpa.domain.Specification;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.ServiceRequestPersistenceEntity;

import java.util.Objects;
import java.util.Set;

/**
 * Criteria API specifications over {@link ServiceRequestPersistenceEntity}.
 *
 * <p>Every listing is scoped first - to the owner who created the requests or to the technician
 * they are assigned to - and the status filter, when one is supplied, is {@code AND}-ed to that
 * scope, so it can only ever narrow the caller's own requests. As in the equipment listing, the
 * {@code WHERE} clause only carries the predicates that restrict something.</p>
 */
public final class ServiceRequestPersistenceSpecifications {

  private static final String OWNER_ID = "ownerId";
  private static final String TECHNICIAN_ID = "technicianId";
  private static final String STATUS = "status";

  private ServiceRequestPersistenceSpecifications() {
  }

  /**
   * Builds the specification behind an owner's listing.
   *
   * @param ownerId  the owner; required
   * @param statuses restrict to these statuses; {@code null} or empty does not restrict
   * @return the specification, to be used for both the rows and their count
   */
  public static Specification<ServiceRequestPersistenceEntity> ownedBy(Long ownerId, Set<ServiceStatus> statuses) {
    return scopedListing(OWNER_ID, Objects.requireNonNull(ownerId, "ownerId must not be null"), statuses);
  }

  /**
   * Builds the specification behind a technician's listing of its assignments.
   *
   * @param technicianId the technician; required
   * @param statuses     restrict to these statuses; {@code null} or empty does not restrict
   * @return the specification, to be used for both the rows and their count
   */
  public static Specification<ServiceRequestPersistenceEntity> assignedTo(
      Long technicianId, Set<ServiceStatus> statuses) {
    return scopedListing(
        TECHNICIAN_ID, Objects.requireNonNull(technicianId, "technicianId must not be null"), statuses);
  }

  private static Specification<ServiceRequestPersistenceEntity> scopedListing(
      String scopeAttribute, Long scopeValue, Set<ServiceStatus> statuses) {
    var statusFilter = statuses == null || statuses.isEmpty() ? null : Set.copyOf(statuses);
    return (root, query, criteriaBuilder) -> {
      var scope = criteriaBuilder.equal(root.get(scopeAttribute), scopeValue);
      if (statusFilter == null) {
        return scope;
      }
      return criteriaBuilder.and(scope, root.get(STATUS).in(statusFilter));
    };
  }
}
