package pe.edu.upc.ice.track.platform.notifications.infrastructure.services;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.profiles.ExternalProfilesServiceForNotifications;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;

import java.util.Optional;

/** Adapter chaining the IAM and Profiles facades: username -> account -> owner profile. */
@Service
public class ExternalProfilesServiceForNotificationsImpl implements ExternalProfilesServiceForNotifications {

  private static final long NOT_FOUND = 0L;

  private final IamContextFacade iamContextFacade;
  private final ProfilesContextFacade profilesContextFacade;

  public ExternalProfilesServiceForNotificationsImpl(
      IamContextFacade iamContextFacade, ProfilesContextFacade profilesContextFacade) {
    this.iamContextFacade = iamContextFacade;
    this.profilesContextFacade = profilesContextFacade;
  }

  @Override
  public Optional<Long> fetchRecipientUserIdByUsername(String username) {
    if (username == null || username.isBlank()) {
      return Optional.empty();
    }
    var userId = iamContextFacade.fetchUserIdByUsername(username);
    if (userId == null || userId == NOT_FOUND) {
      return Optional.empty();
    }
    var ownerId = profilesContextFacade.fetchOwnerIdByUserId(userId);
    return ownerId == null || ownerId == NOT_FOUND ? Optional.empty() : Optional.of(ownerId);
  }
}
