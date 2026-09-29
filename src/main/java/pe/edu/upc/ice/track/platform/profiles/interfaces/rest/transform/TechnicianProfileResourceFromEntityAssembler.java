package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.TechnicianProfileResource;

/**
 * Assembler to convert a {@link TechnicianProfile} aggregate to a {@link TechnicianProfileResource}.
 */
public class TechnicianProfileResourceFromEntityAssembler {
  /**
   * Converts a TechnicianProfile aggregate to a TechnicianProfileResource.
   *
   * @param entity The {@link TechnicianProfile} aggregate to convert.
   * @return The {@link TechnicianProfileResource} resource.
   */
  public static TechnicianProfileResource toResourceFromEntity(TechnicianProfile entity) {
    return new TechnicianProfileResource(
        entity.getUserProfileId(),
        entity.getUserId().userId(),
        entity.getFullName().getFullName(),
        entity.getEmail().getAddress(),
        entity.getPhone().getFullNumber(),
        entity.getAddress().getStreetAddress(),
        entity.getSpeciality().name(),
        entity.getCertificationNumber());
  }
}
