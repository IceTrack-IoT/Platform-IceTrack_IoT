package pe.edu.upc.ice.track.platform.service.request.interfaces.acl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.OwnerId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.EdgeGrantRepository;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ReviewRepository;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ServiceRequestRepository;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ACL facade that exposes Service Request and Feedback Management capabilities to other bounded
 * contexts.
 *
 * <p>This class is the published contract of the Anti-Corruption Layer of the
 * {@code service.request} context. Every parameter and return type is a primitive, a boxed
 * primitive, a {@link List} or {@link Optional} of them, an {@link Instant} or a value record of this
 * package such as {@link ActiveEdgeGrant} - no {@code ServiceRequest} or
 * {@code Review} aggregate, value object, repository or JPA entity appears in its signature.
 * Callers depend on this class alone and must never reach into
 * {@code service.request.domain.*}, {@code service.request.application.*} or
 * {@code service.request.infrastructure.*}. Consumed synchronously by Reporting, by Assets and
 * Monitoring to decide whether a technician may read a unit or a site, and by Monitoring to attribute
 * a maintenance window to the order in progress on its unit.</p>
 *
 * <p>Like {@code AssetContextFacade}, every method answers empty rather than throwing: a
 * {@code null} or non-positive identifier, or an incomplete or inverted window, is answered as
 * "nothing matches". Inputs are checked before any value object is built, because those value
 * objects reject a missing or non-positive identifier with an exception.</p>
 */
@Service
public class ServiceRequestContextFacade {

  /**
   * Statuses in which an assigned technician is still engaged with a request, and may therefore
   * read the unit and the site it was raised for. Mirrors {@link ServiceStatus#isActive()}.
   */
  private static final Set<ServiceStatus> ACTIVE_ASSIGNMENT_STATUSES =
      EnumSet.of(ServiceStatus.PENDING, ServiceStatus.ACCEPTED, ServiceStatus.IN_PROGRESS);

  private final ServiceRequestRepository serviceRequestRepository;
  private final ReviewRepository reviewRepository;
  private final EdgeGrantRepository edgeGrantRepository;

  /**
   * Constructor
   *
   * @param serviceRequestRepository the {@link ServiceRequestRepository} instance
   * @param reviewRepository         the {@link ReviewRepository} instance
   * @param edgeGrantRepository      the {@link EdgeGrantRepository} instance
   */
  public ServiceRequestContextFacade(
      ServiceRequestRepository serviceRequestRepository,
      ReviewRepository reviewRepository,
      EdgeGrantRepository edgeGrantRepository) {
    this.serviceRequestRepository = serviceRequestRepository;
    this.reviewRepository = reviewRepository;
    this.edgeGrantRepository = edgeGrantRepository;
  }

  /**
   * Returns the edge grants that currently open a unit's gateway: neither revoked nor expired.
   *
   * <p>Device Management includes them in the configuration the gateway polls. Only token digests
   * leave this context.</p>
   *
   * @param equipmentId identifier of the unit
   * @return the active grants, oldest first; empty for a missing or non-positive identifier
   */
  @Transactional(readOnly = true)
  public List<ActiveEdgeGrant> fetchActiveEdgeGrants(Long equipmentId) {
    if (!isValidIdentifier(equipmentId)) {
      return List.of();
    }
    return edgeGrantRepository.findActiveByEquipmentId(equipmentId, Instant.now()).stream()
        .map(grant -> new ActiveEdgeGrant(
            grant.getTokenDigest(), grant.getTechnicianProfileId(), grant.getServiceRequestId(), grant.getExpiresAt()))
        .toList();
  }

  /**
   * @param siteId identifier of the site the requests were raised for
   * @param from   inclusive start of the window, matched against the completion instant
   * @param to     inclusive end of the window
   * @return the distinct units of that site with at least one completed preventive maintenance
   *         request in the window; empty when none qualifies
   */
  @Transactional(readOnly = true)
  public List<Long> fetchEquipmentIdsWithCompletedPreventiveMaintenanceInRange(
      Long siteId, Instant from, Instant to) {
    if (siteId == null || !isValidWindow(from, to)) {
      return List.of();
    }
    return serviceRequestRepository
        .findEquipmentIdsCompletedBySiteIdAndTypeInRange(siteId, ServiceType.PREVENTIVE_MAINTENANCE, from, to)
        .stream()
        .map(EquipmentId::equipmentId)
        .toList();
  }

  /**
   * @param technicianProfileId identifier of the technician profile
   * @param from                inclusive start of the window, matched against the completion instant
   * @param to                  inclusive end of the window
   * @return how many requests the technician completed in the window; {@code 0} when none
   */
  @Transactional(readOnly = true)
  public long countCompletedServiceRequestsByTechnicianInRange(Long technicianProfileId, Instant from, Instant to) {
    if (!isValidIdentifier(technicianProfileId) || !isValidWindow(from, to)) {
      return 0L;
    }
    return serviceRequestRepository.countCompletedByTechnicianIdInRange(
        new TechnicianProfileId(technicianProfileId), from, to);
  }

  /**
   * Returns the technician's average review rating across their whole history.
   *
   * <p>A technician who has never been reviewed has no rating, which is not the same as a rating of
   * zero, so this answers {@code null} rather than {@code 0.0}.</p>
   *
   * @param technicianProfileId identifier of the technician profile
   * @return the average rating between 1 and 5, or {@code null} when the technician has no review
   */
  @Transactional(readOnly = true)
  public Double fetchAverageRatingByTechnician(Long technicianProfileId) {
    if (!isValidIdentifier(technicianProfileId)) {
      return null;
    }
    return reviewRepository.findAverageRatingByTechnicianId(new TechnicianProfileId(technicianProfileId))
        .orElse(null);
  }

  /**
   * Tells whether a technician currently holds an active assignment on a unit.
   *
   * <p>An assignment is active while the request is {@code PENDING} (already assigned),
   * {@code ACCEPTED} or {@code IN_PROGRESS}. Other contexts use this to scope what a technician may
   * read: a unit is visible to a technician only while that technician is working on it.</p>
   *
   * @param technicianProfileId identifier of the technician profile
   * @param equipmentId         identifier of the unit
   * @return {@code true} when such an assignment exists; {@code false} for any missing or
   *         non-positive identifier
   */
  @Transactional(readOnly = true)
  public boolean hasActiveAssignmentOnEquipment(Long technicianProfileId, Long equipmentId) {
    if (!isValidIdentifier(technicianProfileId) || !isValidIdentifier(equipmentId)) {
      return false;
    }
    return serviceRequestRepository.existsByEquipmentIdAndTechnicianIdAndStatusIn(
        new EquipmentId(equipmentId), new TechnicianProfileId(technicianProfileId), ACTIVE_ASSIGNMENT_STATUSES);
  }

  /**
   * Tells whether a technician currently holds an active assignment on some unit of a site.
   *
   * @param technicianProfileId identifier of the technician profile
   * @param siteId              identifier of the site
   * @return {@code true} when such an assignment exists; {@code false} for any missing or
   *         non-positive identifier
   */
  @Transactional(readOnly = true)
  public boolean hasActiveAssignmentAtSite(Long technicianProfileId, Long siteId) {
    if (!isValidIdentifier(technicianProfileId) || !isValidIdentifier(siteId)) {
      return false;
    }
    return serviceRequestRepository.existsBySiteIdAndTechnicianIdAndStatusIn(
        siteId, new TechnicianProfileId(technicianProfileId), ACTIVE_ASSIGNMENT_STATUSES);
  }

  /**
   * Tells whether an owner and a technician are currently working together on some request.
   *
   * <p>Profiles uses this to decide whether either side may see the other's contact details: only
   * while a request created by the owner is actively assigned to the technician.</p>
   *
   * @param ownerProfileId      identifier of the owner profile
   * @param technicianProfileId identifier of the technician profile
   * @return {@code true} when such a request exists; {@code false} for any missing or non-positive
   *         identifier
   */
  @Transactional(readOnly = true)
  public boolean hasActiveEngagementBetween(Long ownerProfileId, Long technicianProfileId) {
    if (!isValidIdentifier(ownerProfileId) || !isValidIdentifier(technicianProfileId)) {
      return false;
    }
    return serviceRequestRepository.existsByOwnerIdAndTechnicianIdAndStatusIn(
        new OwnerId(ownerProfileId), new TechnicianProfileId(technicianProfileId), ACTIVE_ASSIGNMENT_STATUSES);
  }

  /**
   * Finds the request that was in progress on a unit at an instant.
   *
   * <p>Monitoring and Alerting uses it to attribute a maintenance window started with the gateway's
   * hardware button to the order a technician was working on. The request may have been completed
   * since: what counts is that its work had started and not yet finished at that instant.</p>
   *
   * @param equipmentId identifier of the unit
   * @param instant     the instant
   * @return the request and its technician, or empty when no work was in progress then, or for a
   *         missing or non-positive identifier or a missing instant
   */
  @Transactional(readOnly = true)
  public Optional<InProgressServiceRequest> findInProgressServiceRequestAt(Long equipmentId, Instant instant) {
    if (!isValidIdentifier(equipmentId) || instant == null) {
      return Optional.empty();
    }
    return serviceRequestRepository.findInProgressAt(new EquipmentId(equipmentId), instant)
        .filter(request -> request.getTechnicianId() != null)
        .map(request -> new InProgressServiceRequest(
            request.getId().serviceRequestId(), request.getTechnicianId().technicianProfileId()));
  }

  /**
   * Counts the requests an owner created, per status.
   *
   * <p>Monitoring and Alerting uses it for the owner dashboard. Every status is answered, in
   * lifecycle order, with a count of zero when no request holds it, so the caller reports the same
   * statuses whatever the owner has.</p>
   *
   * @param ownerProfileId identifier of the owner profile
   * @param siteId         restrict to the requests raised for a single site, may be {@code null}
   * @return one count per status; every count is zero for a missing or non-positive owner identifier
   */
  @Transactional(readOnly = true)
  public List<ServiceRequestStatusCount> countServiceRequestsByStatus(Long ownerProfileId, Long siteId) {
    var ownerId = isValidIdentifier(ownerProfileId) ? new OwnerId(ownerProfileId) : null;
    return Arrays.stream(ServiceStatus.values())
        .map(status -> new ServiceRequestStatusCount(
            status.name(),
            status.isActive(),
            ownerId == null ? 0L : serviceRequestRepository.countByOwnerIdAndStatus(ownerId, siteId, status)))
        .toList();
  }

  /**
   * Returns a request with its status history, its intervention and its review, whatever its owner
   * and status.
   *
   * <p>Reporting uses it to produce the service document of a completed request; it checks itself
   * that the request belongs to the caller and is {@code COMPLETED}.</p>
   *
   * @param serviceRequestId identifier of the request
   * @return the request, or empty for a missing or non-positive identifier or a request that does not
   *         exist
   */
  @Transactional(readOnly = true)
  public Optional<ServiceInterventionRecord> fetchServiceInterventionRecord(Long serviceRequestId) {
    if (!isValidIdentifier(serviceRequestId)) {
      return Optional.empty();
    }
    var id = new ServiceRequestId(serviceRequestId);
    return serviceRequestRepository.findById(id).map(request -> {
      var intervention = request.getIntervention();
      var review = reviewRepository.findByServiceRequestId(id).orElse(null);
      return new ServiceInterventionRecord(
          serviceRequestId,
          request.getOwnerId().ownerId(),
          request.getEquipmentId().equipmentId(),
          request.getSiteId().siteId(),
          request.getTechnicianId() == null ? null : request.getTechnicianId().technicianProfileId(),
          request.getType().name(),
          request.getPriority().name(),
          request.getStatus().name(),
          request.getDescription(),
          request.getCreatedAt(),
          request.getStartedAt(),
          request.getCompletedAt(),
          request.getHistory().stream()
              .map(transition -> new ServiceInterventionRecord.StatusChange(
                  transition.getFromStatus() == null ? null : transition.getFromStatus().name(),
                  transition.getToStatus().name(),
                  transition.getOccurredAt(),
                  transition.getReason()))
              .toList(),
          intervention == null ? null : new ServiceInterventionRecord.InterventionDetails(
              intervention.getDiagnosis(),
              intervention.getActionsTaken(),
              intervention.getSpareParts(),
              intervention.getPhotoEvidence(),
              intervention.getStartedAt(),
              intervention.getCompletedAt()),
          review == null ? null : new ServiceInterventionRecord.ReviewDetails(
              review.getRating().communication(),
              review.getRating().efficacy(),
              review.getRating().performance(),
              review.getRating().average(),
              review.getComment(),
              review.getCreatedAt()));
    });
  }

  private static boolean isValidIdentifier(Long identifier) {
    return identifier != null && identifier > 0;
  }

  private static boolean isValidWindow(Instant from, Instant to) {
    return from != null && to != null && !from.isAfter(to);
  }
}
