package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;

import java.util.Objects;

/**
 * Command to create the owner profile of a newly registered platform account.
 *
 * <p>Raised by the Anti-Corruption Layer once the values received from the registering context
 * have been translated into profiles domain value objects.</p>
 *
 * @param profileCreationData the validated shared creation data; required
 * @param ruc                 the owner's taxpayer registration number; required
 */
public record CreateOwnerProfileCommand(ProfileCreationData profileCreationData, Ruc ruc) {

  /**
   * Validates that both components were supplied.
   */
  public CreateOwnerProfileCommand {
    Objects.requireNonNull(profileCreationData, "profileCreationData must not be null");
    Objects.requireNonNull(ruc, "ruc must not be null");
  }
}
