package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.ServiceRequestPersistenceEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link ServiceRequestPersistenceEntity}.
 *
 * <p>A single request is always turned into a whole aggregate, so its finder fetches the history
 * and the intervention in the same query. The paged listings go through
 * {@link JpaSpecificationExecutor} with the specifications built by
 * {@code ServiceRequestPersistenceSpecifications}, and rely on batch fetching rather than an entity
 * graph, since fetching a collection together with a page would make Hibernate page in memory.
 * There is deliberately no repository for the transitions nor for the interventions: both are only
 * ever written through their request.</p>
 */
@Repository
public interface ServiceRequestPersistenceRepository
    extends JpaRepository<ServiceRequestPersistenceEntity, Long>,
    JpaSpecificationExecutor<ServiceRequestPersistenceEntity> {

  /**
   * Finds a request together with its history and its intervention.
   *
   * @param id the request identifier
   * @return the row, or empty when no request carries that identifier
   */
  @EntityGraph(attributePaths = {"history", "intervention"})
  Optional<ServiceRequestPersistenceEntity> findWithDetailsById(Long id);

  /**
   * Finds the requests of a unit in a status, most recently completed first, with their
   * intervention.
   *
   * @param equipmentId the unit
   * @param status      the status to match
   * @return the matching rows
   */
  @EntityGraph(attributePaths = "intervention")
  List<ServiceRequestPersistenceEntity> findByEquipmentIdAndStatusOrderByCompletedAtDescIdDesc(
      Long equipmentId, ServiceStatus status);

  /**
   * Tells whether a technician is assigned to a request on a unit in one of the given statuses.
   *
   * @param equipmentId  the unit
   * @param technicianId the technician
   * @param statuses     the statuses that count
   * @return {@code true} when such a request exists
   */
  boolean existsByEquipmentIdAndTechnicianIdAndStatusIn(
      Long equipmentId, Long technicianId, Collection<ServiceStatus> statuses);

  /**
   * Tells whether an intervention already carries a client identifier.
   *
   * @param clientGeneratedId the identifier
   * @return {@code true} when some request holds an intervention with that identifier
   */
  boolean existsByInterventionClientGeneratedId(UUID clientGeneratedId);
}
