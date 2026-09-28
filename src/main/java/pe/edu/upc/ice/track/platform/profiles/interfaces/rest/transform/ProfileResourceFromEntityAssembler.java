package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.ProfileResource;

/**
 * Assembler to convert a Profile entity to a ProfileResource.
 */
public class ProfileResourceFromEntityAssembler {
  /**
   * Converts a Profile entity to a ProfileResource.
   *
   * <p>Only the role specific fields of the concrete profile are populated; the others are
   * rendered as {@code null}.</p>
   *
   * @param entity The {@link Profile} entity to convert.
   * @return The {@link ProfileResource} resource.
   */
  public static ProfileResource toResourceFromEntity(Profile entity) {
    Long ruc = null;
    String speciality = null;
    String certificationNumber = null;
    switch (entity) {
      case OwnerProfile ownerProfile -> ruc = ownerProfile.getRuc().value();
      case TechnicianProfile technicianProfile -> {
        speciality = technicianProfile.getQualification().speciality();
        certificationNumber = technicianProfile.getQualification().certificationNumber();
      }
    }
    return new ProfileResource(
        entity.getId(),
        entity.getUserId().userId(),
        entity.getFullName().getFullName(),
        entity.getEmail().getAddress(),
        entity.getRoleName(),
        entity.getPhone().getFullNumber(),
        entity.getAddress().getStreetAddress(),
        entity.getAuxiliaryData(),
        ruc,
        speciality,
        certificationNumber
    );
  }
}
