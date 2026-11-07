package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.ServiceRequestPersistenceEntity;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.projections.EquipmentIdProjection;

import java.time.Instant;
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
   * Tells whether a technician is assigned to a request raised for a site in one of the given
   * statuses.
   *
   * @param siteId       the site
   * @param technicianId the technician
   * @param statuses     the statuses that count
   * @return {@code true} when such a request exists
   */
  boolean existsBySiteIdAndTechnicianIdAndStatusIn(
      Long siteId, Long technicianId, Collection<ServiceStatus> statuses);

  /**
   * Tells whether an owner has a request assigned to a technician in one of the given statuses.
   *
   * @param ownerId      the owner
   * @param technicianId the technician
   * @param statuses     the statuses that count
   * @return {@code true} when such a request exists
   */
  boolean existsByOwnerIdAndTechnicianIdAndStatusIn(
      Long ownerId, Long technicianId, Collection<ServiceStatus> statuses);

  /**
   * Projects the distinct units of a site that have a request of a type, in a status, completed
   * within a window. The closed {@link EquipmentIdProjection} limits the selection to the unit
   * identifier, so no entity, history or intervention is loaded and {@code Distinct} applies to
   * the identifiers.
   *
   * @param siteId the site
   * @param type   the kind of work
   * @param status the status to match
   * @param from   inclusive start of the window
   * @param to     inclusive end of the window
   * @return the matching unit identifiers
   */
  List<EquipmentIdProjection> findDistinctEquipmentIdBySiteIdAndTypeAndStatusAndCompletedAtBetween(
      Long siteId, ServiceType type, ServiceStatus status, Instant from, Instant to);

  /**
   * Counts the requests of a technician, in a status, completed within a window.
   *
   * @param technicianId the technician
   * @param status       the status to match
   * @param from         inclusive start of the window
   * @param to           inclusive end of the window
   * @return the number of matching rows
   */
  long countByTechnicianIdAndStatusAndCompletedAtBetween(
      Long technicianId, ServiceStatus status, Instant from, Instant to);

  /**
   * Tells whether an intervention already carries a client identifier.
   *
   * @param clientGeneratedId the identifier
   * @return {@code true} when some request holds an intervention with that identifier
   */
  boolean existsByInterventionClientGeneratedId(UUID clientGeneratedId);

  /**
   * Finds the requests of a unit, in one of the given statuses, started at or before an instant,
   * latest started first.
   *
   * @param equipmentId the unit
   * @param statuses    the statuses that count
   * @param instant     the instant the start must not follow
   * @return the matching rows
   */
  List<ServiceRequestPersistenceEntity> findByEquipmentIdAndStatusInAndStartedAtLessThanEqualOrderByStartedAtDesc(
      Long equipmentId, Collection<ServiceStatus> statuses, Instant instant);
}
