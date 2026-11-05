package pe.edu.upc.ice.track.platform.service.request.interfaces.acl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ReviewRepository;
import pe.edu.upc.ice.track.platform.service.request.domain.repositories.ServiceRequestRepository;

import java.time.Instant;
import java.util.List;

/**
 * ACL facade that exposes Service Request and Feedback Management capabilities to other bounded
 * contexts.
 *
 * <p>This class is the published contract of the Anti-Corruption Layer of the
 * {@code service.request} context. Every parameter and return type is a primitive, a boxed
 * primitive, a {@link List} of them or an {@link Instant} - no {@code ServiceRequest} or
 * {@code Review} aggregate, value object, repository or JPA entity appears in its signature.
 * Callers depend on this class alone and must never reach into
 * {@code service.request.domain.*}, {@code service.request.application.*} or
 * {@code service.request.infrastructure.*}. Consumed synchronously by Reporting.</p>
 *
 * <p>Like {@code AssetContextFacade}, every method answers empty rather than throwing: a
 * {@code null} or non-positive identifier, or an incomplete or inverted window, is answered as
 * "nothing matches". Inputs are checked before any value object is built, because those value
 * objects reject a missing or non-positive identifier with an exception.</p>
 */
@Service
public class ServiceRequestContextFacade {

  private final ServiceRequestRepository serviceRequestRepository;
  private final ReviewRepository reviewRepository;

  /**
   * Constructor
   *
   * @param serviceRequestRepository the {@link ServiceRequestRepository} instance
   * @param reviewRepository         the {@link ReviewRepository} instance
   */
  public ServiceRequestContextFacade(
      ServiceRequestRepository serviceRequestRepository, ReviewRepository reviewRepository) {
    this.serviceRequestRepository = serviceRequestRepository;
    this.reviewRepository = reviewRepository;
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

  private static boolean isValidIdentifier(Long identifier) {
    return identifier != null && identifier > 0;
  }

  private static boolean isValidWindow(Instant from, Instant to) {
    return from != null && to != null && !from.isAfter(to);
  }
}
