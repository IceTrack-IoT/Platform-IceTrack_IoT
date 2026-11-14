package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.OwnerProfileResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.authorization.ProfileView;

/**
 * Assembler to convert an {@link OwnerProfile} aggregate to an {@link OwnerProfileResource}.
 */
public class OwnerProfileResourceFromEntityAssembler {

  /**
   * Converts an owner profile into the resource a caller may see, never widening what
   * {@code view} allows: fields hidden by the view are sent as {@code null}.
   *
   * @param entity the owner profile
   * @param view   how much of it the caller may see; {@code CONTACT} hides the address, the RUC and the
   *               version
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
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  /**
   * Converts an owner profile into the resource its owner sees, with the address both composed and in
   * its parts.
   *
   * @param entity the owner profile
   * @return the resource
   */
  public static OwnerProfileResource toResourceFromEntity(OwnerProfile entity) {
    var address = entity.getAddress();
    return new OwnerProfileResource(
        entity.getUserProfileId(),
        entity.getUserId().userId(),
        entity.getFullName().getFullName(),
        entity.getEmail().getAddress(),
        entity.getPhone().getFullNumber(),
        address.getStreetAddress(),
        address.street(),
        address.number(),
        address.city(),
        address.postalCode(),
        address.country(),
        entity.getRuc().value(),
        entity.getVersion() == null ? 0L : entity.getVersion());
  }
}
