package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.OwnerProfileResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.authorization.ProfileView;

/**
 * Assembler to convert an {@link OwnerProfile} aggregate to an {@link OwnerProfileResource}.
 */
public class OwnerProfileResourceFromEntityAssembler {
  /**
   * Converts an OwnerProfile aggregate to an OwnerProfileResource.
   *
   * @param entity The {@link OwnerProfile} aggregate to convert.
   * @return The {@link OwnerProfileResource} resource.
   */
  /**
   * Converts an owner profile into the resource a caller may see, never widening what
   * {@code view} allows: fields hidden by the view are sent as {@code null}.
   *
   * @param entity the owner profile
   * @param view   how much of it the caller may see; {@code CONTACT} hides the address and the RUC
   * @return the resource
   */
  public static OwnerProfileResource toResourceFromEntity(OwnerProfile entity, ProfileView view) {
    if (view == ProfileView.FULL) {
      return toResourceFromEntity(entity);
    }
    return new OwnerProfileResource(
        entity.getUserProfileId(),
        entity.getUserId().userId(),
        entity.getFullName().getFullName(),
        view == ProfileView.CONTACT ? entity.getEmail().getAddress() : null,
        view == ProfileView.CONTACT ? entity.getPhone().getFullNumber() : null,
        null,
        null);
  }

  public static OwnerProfileResource toResourceFromEntity(OwnerProfile entity) {
    return new OwnerProfileResource(
        entity.getUserProfileId(),
        entity.getUserId().userId(),
        entity.getFullName().getFullName(),
        entity.getEmail().getAddress(),
        entity.getPhone().getFullNumber(),
        entity.getAddress().getStreetAddress(),
        entity.getRuc().value());
  }
}
