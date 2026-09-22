package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateProfileCommand;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.CreateProfileResource;

/**
 * Assembler to convert a CreateProfileResource to a CreateProfileCommand.
 */
public class CreateProfileCommandFromResourceAssembler {
  /**
   * Converts a CreateProfileResource to a CreateProfileCommand.
   * @param resource The {@link CreateProfileResource} resource to convert.
   * @return The {@link CreateProfileCommand} command.
   */
  public static CreateProfileCommand toCommandFromResource(CreateProfileResource resource) {
    return new CreateProfileCommand(
        resource.userId(),
        resource.firstName(),
        resource.lastName(),
        resource.email(),
        resource.countryCode(),
        resource.phoneNumber(),
        resource.street(),
        resource.number(),
        resource.city(),
        resource.postalCode(),
        resource.country()
    );
  }
}
