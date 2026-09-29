package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateTechnicianCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Speciality;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.UpdateTechnicianResource;

/**
 * Assembler that converts an {@link UpdateTechnicianResource} into an
 * {@link UpdateTechnicianCommand}.
 */
public class UpdateTechnicianCommandFromResourceAssembler {

  /**
   * Converts the technician update payload into its command representation.
   *
   * <p>Each value object validates itself on construction; a violation surfaces as an
   * {@link IllegalArgumentException}, rendered as a 400 response. The certification number format
   * is enforced by the {@code TechnicianProfile} aggregate itself.</p>
   *
   * @param technicianId identifier of the technician to update, taken from the request path
   * @param resource     the {@link UpdateTechnicianResource} resource to convert
   * @return the {@link UpdateTechnicianCommand} command
   * @throws IllegalArgumentException when a value violates a domain invariant
   */
  public static UpdateTechnicianCommand toCommandFromResource(Long technicianId, UpdateTechnicianResource resource) {
    return new UpdateTechnicianCommand(
        technicianId,
        PersonName.fromDisplayName(resource.fullName()),
        Phone.fromString(resource.phone()),
        new StreetAddress(resource.street(), resource.number(), resource.city(), resource.postalCode(), resource.country()),
        new Speciality(resource.speciality()),
        resource.certificationNumber());
  }
}
