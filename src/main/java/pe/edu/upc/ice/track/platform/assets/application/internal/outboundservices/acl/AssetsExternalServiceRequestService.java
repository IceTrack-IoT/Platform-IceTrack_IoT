package pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;

import java.util.Optional;

/**
 * Outbound service through which the {@code assets} bounded context decides whether a caller acting
 * as a technician may read a unit or a site.
 *
 * <p>A technician does not own anything in this context, so ownership cannot scope what it reads.
 * What does is its work: a technician may read a unit, and the site it is installed at, only while
 * it holds an active assignment there ({@code PENDING} once assigned, {@code ACCEPTED} or
 * {@code IN_PROGRESS}). Anything else is answered as "not found" by the query services, so a
 * technician cannot harvest identifiers or site contact details of other tenants.</p>
 *
 * <p>Chains three published facades, all made of primitives and strings: the caller's username is
 * resolved to an account through {@link IamContextFacade}, that account to a technician profile
 * through {@link ProfilesContextFacade}, and the assignment is checked through
 * {@link ServiceRequestContextFacade}.</p>
 */
@Service
public class AssetsExternalServiceRequestService {

  /** Sentinel the IAM and profiles facades return when nothing matches. */
  private static final long NOT_FOUND = 0L;

  private final IamContextFacade iamContextFacade;
  private final ProfilesContextFacade profilesContextFacade;
  private final ServiceRequestContextFacade serviceRequestContextFacade;

  /**
   * Creates the outbound service.
   *
   * @param iamContextFacade            the inbound ACL facade of the IAM bounded context
   * @param profilesContextFacade       the inbound ACL facade of the profiles bounded context
   * @param serviceRequestContextFacade the inbound ACL facade of the service request bounded context
   */
  public AssetsExternalServiceRequestService(
      IamContextFacade iamContextFacade,
      ProfilesContextFacade profilesContextFacade,
      ServiceRequestContextFacade serviceRequestContextFacade) {
    this.iamContextFacade = iamContextFacade;
    this.profilesContextFacade = profilesContextFacade;
    this.serviceRequestContextFacade = serviceRequestContextFacade;
  }

  /**
   * Tells whether the technician behind a username is actively assigned to a unit.
   *
   * @param username    username of the authenticated caller
   * @param equipmentId identifier of the unit
   * @return {@code true} only when the caller holds a technician profile with an active assignment
   *         on that unit
   */
  public boolean isTechnicianAssignedToEquipment(String username, Long equipmentId) {
    return fetchTechnicianIdByUsername(username)
        .map(technicianId -> serviceRequestContextFacade.hasActiveAssignmentOnEquipment(technicianId, equipmentId))
        .orElse(false);
  }

  /**
   * Tells whether the technician behind a username is actively assigned to some unit of a site.
   *
   * @param username username of the authenticated caller
   * @param siteId   identifier of the site
   * @return {@code true} only when the caller holds a technician profile with an active assignment
   *         at that site
   */
  public boolean isTechnicianAssignedToSite(String username, Long siteId) {
    return fetchTechnicianIdByUsername(username)
        .map(technicianId -> serviceRequestContextFacade.hasActiveAssignmentAtSite(technicianId, siteId))
        .orElse(false);
  }

  private Optional<Long> fetchTechnicianIdByUsername(String username) {
    if (username == null || username.isBlank()) {
      return Optional.empty();
    }
    var userId = iamContextFacade.fetchUserIdByUsername(username);
    if (userId == null || userId == NOT_FOUND) {
      return Optional.empty();
    }
    var technicianId = profilesContextFacade.fetchTechnicianIdByUserId(userId);
    return technicianId == null || technicianId == NOT_FOUND ? Optional.empty() : Optional.of(technicianId);
  }
}
