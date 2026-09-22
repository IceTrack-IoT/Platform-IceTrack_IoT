package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.ProfileResource;

/**
 * Assembler to convert a Profile entity to a ProfileResource.
 */
public class ProfileResourceFromEntityAssembler {
  /**
   * Converts a Profile entity to a ProfileResource.
   *
   * <p>Contact details are optional on a profile, so the phone number and the street address are
   * rendered as {@code null} when the owner has not supplied them yet.</p>
   *
   * @param entity The {@link Profile} entity to convert.
   * @return The {@link ProfileResource} resource.
   */
  public static ProfileResource toResourceFromEntity(Profile entity) {
    var userId = entity.getUserId();
    var phone = entity.getPhone();
    var address = entity.getAddress();
    return new ProfileResource(
        entity.getId(),
        userId == null ? null : userId.userId(),
        entity.getFullName().getFullName(),
        entity.getEmail().getAddress(),
        entity.getRoleName(),
        phone == null ? null : phone.getFullNumber(),
        address == null ? null : address.getStreetAddress(),
        entity.getAuxiliaryData()
    );
  }
}
