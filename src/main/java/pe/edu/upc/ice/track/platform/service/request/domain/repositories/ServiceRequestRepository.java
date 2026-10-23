package pe.edu.upc.ice.track.platform.service.request.domain.repositories;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestPage;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service request repository port.
 *
 * <p>Implemented by {@code ServiceRequestRepositoryImpl} in the infrastructure layer, which is also
 * where the domain events a request registered are published, once the write reached the
 * database.</p>
 *
 * <p>Every lookup answers the whole aggregate - status history and intervention included - and
 * leaves scoping to the caller: the application layer filters by {@code isOwnedBy} or
 * {@code isAssignedTo}, or passes the scope to the listing methods, which apply it in the
 * database.</p>
 */
public interface ServiceRequestRepository {

  /**
   * Find a request by its identifier, whoever its parties are.
   *
   * @param serviceRequestId the request identifier
   * @return the request, or empty when none carries that identifier
   */
  Optional<ServiceRequest> findById(ServiceRequestId serviceRequestId);

  /**
   * Find one page of the requests an owner created, newest first.
   *
   * @param ownerId  the owner; required
   * @param statuses restrict to these statuses; empty does not restrict
   * @param page     zero-based page index; not negative
   * @param size     maximum number of requests the page may hold; strictly positive
   * @return the requests of that page and the total count
   */
  ServiceRequestPage findPageByOwnerId(OwnerId ownerId, Set<ServiceStatus> statuses, int page, int size);

  /**
   * Find one page of the requests currently assigned to a technician, newest first.
   *
   * @param technicianId the technician; required
   * @param statuses     restrict to these statuses; empty does not restrict
   * @param page         zero-based page index; not negative
   * @param size         maximum number of requests the page may hold; strictly positive
   * @return the requests of that page and the total count
   */
  ServiceRequestPage findPageByTechnicianId(
      TechnicianProfileId technicianId, Set<ServiceStatus> statuses, int page, int size);

  /**
   * Find the completed requests of a unit, most recently completed first. Each of them holds the
   * intervention that completed it.
   *
   * @param equipmentId the unit
   * @return the completed requests, empty when the unit was never serviced
   */
  List<ServiceRequest> findCompletedByEquipmentId(EquipmentId equipmentId);

  /**
   * Check whether a technician is assigned to some request on a unit in one of the given statuses.
   *
   * @param equipmentId  the unit
   * @param technicianId the technician
   * @param statuses     the statuses that count; required and not empty
   * @return {@code true} when such a request exists
   */
  boolean existsByEquipmentIdAndTechnicianIdAndStatusIn(
      EquipmentId equipmentId, TechnicianProfileId technicianId, Set<ServiceStatus> statuses);

  /**
   * Check whether an intervention client identifier is already used by some request.
   *
   * <p>Backs the uniqueness rule on {@code client_generated_id}: the check runs before the insert so
   * the caller gets a conflict rather than a constraint violation, while the unique index remains the
   * real guarantee under concurrency.</p>
   *
   * @param clientGeneratedId the identifier
   * @return {@code true} when an intervention already carries it
   */
  boolean existsByInterventionClientGeneratedId(UUID clientGeneratedId);

  /**
   * Persist a request, inserting it when it is new and updating it otherwise.
   *
   * <p>An update is version-checked: a request read before another transaction changed it is
   * rejected with an {@code OptimisticLockingFailureException} rather than overwriting that
   * change.</p>
   *
   * @param serviceRequest the request to persist
   * @return the persisted request, carrying its identity and its new version
   */
  ServiceRequest save(ServiceRequest serviceRequest);
}
