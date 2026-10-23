package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.adapters;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestPage;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ServiceRequestRepository;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.assemblers.ServiceRequestPersistenceAssembler;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.ServiceRequestPersistenceEntity;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.repositories.ServiceRequestPersistenceRepository;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.specifications.ServiceRequestPersistenceSpecifications;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository adapter that bridges the {@link ServiceRequestRepository} port with Spring Data JPA.
 *
 * <p>An existing request is saved by updating the managed entity it was loaded from, never by
 * merging a freshly built one, exactly as {@code DashboardConfigRepositoryImpl} does: that keeps the
 * managed {@code history} collection in place, so appending a transition inserts one row instead of
 * rewriting the history.</p>
 *
 * <p><strong>Optimistic locking.</strong> The version of the aggregate is compared with the version
 * of the managed entity before anything is copied, so a request read in another transaction is
 * rejected outright; a change committed by a concurrent transaction after this one read the row is
 * caught by the {@code @Version} check of the {@code UPDATE} itself. Both surface as an
 * {@link OptimisticLockingFailureException}, which {@code GlobalExceptionHandler} answers with
 * {@code 409 CONCURRENT_MODIFICATION_CONFLICT}.</p>
 *
 * <p>This is also the event-publishing boundary: the events a change registered are dispatched right
 * after the row is flushed, so an event published to the rest of the platform is always one that
 * reached the database, and the creation event of a new request is raised on the rehydrated copy,
 * since it carries the identity the incoming aggregate does not have yet.</p>
 */
@Repository
public class ServiceRequestRepositoryImpl implements ServiceRequestRepository {

  /**
   * Newest first, then by identifier, because a listing paginated without a total order silently
   * drops and repeats rows from one page to the next.
   */
  private static final Sort LISTING_ORDER = Sort.by(Sort.Order.desc("requestedAt"), Sort.Order.desc("id"));

  private final ServiceRequestPersistenceRepository serviceRequestPersistenceRepository;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructor
   *
   * @param serviceRequestPersistenceRepository the Spring Data repository
   * @param eventPublisher                      the Spring event publisher used to dispatch domain events
   */
  public ServiceRequestRepositoryImpl(
      ServiceRequestPersistenceRepository serviceRequestPersistenceRepository,
      ApplicationEventPublisher eventPublisher) {
    this.serviceRequestPersistenceRepository = serviceRequestPersistenceRepository;
    this.eventPublisher = eventPublisher;
  }

  // inherited javadoc
  @Override
  public Optional<ServiceRequest> findById(ServiceRequestId serviceRequestId) {
    if (serviceRequestId == null) {
      return Optional.empty();
    }
    return serviceRequestPersistenceRepository.findWithDetailsById(serviceRequestId.serviceRequestId())
        .map(ServiceRequestPersistenceAssembler::toDomainFromPersistence);
  }

  // inherited javadoc
  @Override
  public ServiceRequestPage findPageByOwnerId(OwnerId ownerId, Set<ServiceStatus> statuses, int page, int size) {
    if (ownerId == null) {
      return ServiceRequestPage.empty(page, size);
    }
    return findPage(ServiceRequestPersistenceSpecifications.ownedBy(ownerId.ownerId(), statuses), page, size);
  }

  // inherited javadoc
  @Override
  public ServiceRequestPage findPageByTechnicianId(
      TechnicianProfileId technicianId, Set<ServiceStatus> statuses, int page, int size) {
    if (technicianId == null) {
      return ServiceRequestPage.empty(page, size);
    }
    return findPage(
        ServiceRequestPersistenceSpecifications.assignedTo(technicianId.technicianProfileId(), statuses), page, size);
  }

  // inherited javadoc
  @Override
  public List<ServiceRequest> findCompletedByEquipmentId(EquipmentId equipmentId) {
    if (equipmentId == null) {
      return List.of();
    }
    return serviceRequestPersistenceRepository
        .findByEquipmentIdAndStatusOrderByCompletedAtDescIdDesc(equipmentId.equipmentId(), ServiceStatus.COMPLETED)
        .stream()
        .map(ServiceRequestPersistenceAssembler::toDomainFromPersistence)
        .toList();
  }

  // inherited javadoc
  @Override
  public boolean existsByEquipmentIdAndTechnicianIdAndStatusIn(
      EquipmentId equipmentId, TechnicianProfileId technicianId, Set<ServiceStatus> statuses) {
    if (equipmentId == null || technicianId == null || statuses == null || statuses.isEmpty()) {
      return false;
    }
    return serviceRequestPersistenceRepository.existsByEquipmentIdAndTechnicianIdAndStatusIn(
        equipmentId.equipmentId(), technicianId.technicianProfileId(), statuses);
  }

  // inherited javadoc
  @Override
  public boolean existsByInterventionClientGeneratedId(UUID clientGeneratedId) {
    return clientGeneratedId != null
        && serviceRequestPersistenceRepository.existsByInterventionClientGeneratedId(clientGeneratedId);
  }

  /**
   * Persists a request and dispatches whatever domain events the change raised.
   *
   * @param serviceRequest the request to persist
   * @return the persisted request
   * @throws OptimisticLockingFailureException when the request changed or disappeared after it was
   *                                           read
   */
  @Override
  @Transactional
  public ServiceRequest save(ServiceRequest serviceRequest) {
    var isNew = serviceRequest.getId() == null;
    var entity = isNew ? new ServiceRequestPersistenceEntity() : managedEntityOf(serviceRequest);
    ServiceRequestPersistenceAssembler.copyToPersistence(serviceRequest, entity);
    // Flushed so that a version conflict or a unique violation surfaces here, inside the caller's
    // transaction, and so the appended transitions have their identities before mapping back.
    var savedEntity = serviceRequestPersistenceRepository.saveAndFlush(entity);
    var savedRequest = ServiceRequestPersistenceAssembler.toDomainFromPersistence(savedEntity);
    publishPendingEvents(serviceRequest);
    if (isNew) {
      savedRequest.onCreated();
      publishPendingEvents(savedRequest);
    }
    return savedRequest;
  }

  /**
   * Loads the managed entity of a persisted request and checks it is the version the request was
   * read at.
   *
   * @param serviceRequest the persisted request
   * @return the managed entity, with its history and intervention loaded
   * @throws OptimisticLockingFailureException when the row is gone or carries another version
   */
  private ServiceRequestPersistenceEntity managedEntityOf(ServiceRequest serviceRequest) {
    var serviceRequestId = serviceRequest.getId().serviceRequestId();
    var entity = serviceRequestPersistenceRepository.findWithDetailsById(serviceRequestId)
        .orElseThrow(() -> new OptimisticLockingFailureException(
            "Service request %s was deleted after it was read".formatted(serviceRequestId)));
    if (!Objects.equals(entity.getVersion(), serviceRequest.getVersion())) {
      throw new OptimisticLockingFailureException(
          "Service request %s was modified after it was read".formatted(serviceRequestId));
    }
    return entity;
  }

  /**
   * Returns one page of requests, with the count of every matching request.
   *
   * <p>The rows and the count come from the same specification, so the total can never disagree with
   * the filter the rows were selected by. A page whose first row lies beyond what a JDBC offset can
   * address is necessarily past the end, so it is answered empty, with the real total.</p>
   */
  private ServiceRequestPage findPage(
      Specification<ServiceRequestPersistenceEntity> specification, int page, int size) {
    if ((long) page * size > Integer.MAX_VALUE) {
      return new ServiceRequestPage(List.of(), page, size, serviceRequestPersistenceRepository.count(specification));
    }
    var slice = serviceRequestPersistenceRepository.findAll(specification, PageRequest.of(page, size, LISTING_ORDER));
    var content = slice.getContent().stream()
        .map(ServiceRequestPersistenceAssembler::toDomainFromPersistence)
        .toList();
    return new ServiceRequestPage(content, page, size, slice.getTotalElements());
  }

  /**
   * Dispatches the events the aggregate registered, then clears them so a later save of the same
   * aggregate does not announce the same change twice.
   *
   * @param serviceRequest the aggregate that was just persisted
   */
  private void publishPendingEvents(ServiceRequest serviceRequest) {
    serviceRequest.domainEvents().forEach(eventPublisher::publishEvent);
    serviceRequest.clearDomainEvents();
  }
}
