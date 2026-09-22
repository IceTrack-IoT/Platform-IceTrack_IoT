package pe.edu.upc.ice.track.platform.iam.infrastructure.profiles.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.profiles.ExternalProfileService;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

import java.util.Optional;

/**
 * Infrastructure adapter that fulfils the {@link ExternalProfileService} port by calling the
 * {@code profiles} bounded context through its ACL facade.
 *
 * <p>This class is the boundary itself, and deliberately the only class in {@code iam} that
 * imports anything from {@code profiles}. What it imports is
 * {@link ProfilesContextFacade} - an interface in the profiles <em>interfaces</em> layer whose
 * whole signature is primitives and {@link String}s. No profiles aggregate, value object,
 * repository, command or JPA entity is reachable from here, and nothing from
 * {@code profiles.domain.*} is on the import list.</p>
 *
 * <p>The call is an ordinary in-process method invocation, so it participates in the caller's
 * transaction: a registration and its profile either both commit or both roll back. Should the
 * two contexts ever be split into separate deployables, only this adapter changes - it would
 * issue a remote call and the IAM application layer would not notice.</p>
 */
@Service
@Slf4j
public class ExternalProfileServiceImpl implements ExternalProfileService {

  /**
   * Sentinel the profiles facade returns when no profile could be found or created.
   */
  private static final long NO_PROFILE = 0L;

  private final ProfilesContextFacade profilesContextFacade;

  /**
   * Creates the adapter.
   *
   * @param profilesContextFacade the inbound ACL facade of the profiles bounded context
   */
  public ExternalProfileServiceImpl(ProfilesContextFacade profilesContextFacade) {
    this.profilesContextFacade = profilesContextFacade;
  }

  // inherited javadoc
  @Override
  public Optional<Long> fetchOrCreateProfile(Long userId, String fullName, String email, String role) {
    return fetchOrCreateProfile(userId, fullName, email, null, role, null);
  }

  // inherited javadoc
  @Override
  public Optional<Long> fetchOrCreateProfile(
      Long userId, String fullName, String email, String phone, String role, String auxiliaryData) {
    if (userId == null) {
      log.warn("Refusing to provision a profile without a user identifier");
      return Optional.empty();
    }

    // Fetch first so that a repeated sign-in never attempts a create. The facade is idempotent
    // as well, so this is a fast path rather than the only safeguard.
    var existingProfileId = profilesContextFacade.fetchProfileIdByUserId(userId);
    if (existingProfileId != null && existingProfileId != NO_PROFILE) {
      return Optional.of(existingProfileId);
    }

    var profileId = profilesContextFacade.createProfile(userId, fullName, email, phone, role, auxiliaryData);
    if (profileId == null || profileId == NO_PROFILE) {
      log.warn("The profiles context did not provision a profile for user {}", userId);
      return Optional.empty();
    }
    log.info("Provisioned profile {} for user {}", profileId, userId);
    return Optional.of(profileId);
  }
}
