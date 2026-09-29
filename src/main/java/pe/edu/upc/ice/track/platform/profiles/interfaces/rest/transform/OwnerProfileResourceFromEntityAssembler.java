package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.OwnerProfileResource;

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
