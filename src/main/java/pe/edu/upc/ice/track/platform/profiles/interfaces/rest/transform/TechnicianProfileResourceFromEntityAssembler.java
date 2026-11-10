package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.TechnicianProfileResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.authorization.ProfileView;

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
  /**
   * Converts a technician profile into the resource a caller may see, never widening what
   * {@code view} allows: fields hidden by the view are sent as {@code null}.
   *
   * @param entity the technician profile
   * @param view   how much of it the caller may see; {@code CONTACT} hides the home address, and
   *               {@code PUBLIC} also hides the email and the phone
   * @return the resource
   */
  public static TechnicianProfileResource toResourceFromEntity(TechnicianProfile entity, ProfileView view) {
    if (view == ProfileView.FULL) {
      return toResourceFromEntity(entity);
    }
    var showContact = view == ProfileView.CONTACT;
    return new TechnicianProfileResource(
        entity.getUserProfileId(),
        entity.getUserId().userId(),
        entity.getFullName().getFullName(),
        showContact ? entity.getEmail().getAddress() : null,
        showContact ? entity.getPhone().getFullNumber() : null,
        null,
        entity.getSpeciality().name(),
        entity.getCertificationNumber());
  }

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
