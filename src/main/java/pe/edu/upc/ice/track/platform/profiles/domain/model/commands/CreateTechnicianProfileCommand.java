package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TechnicianQualification;

import java.util.Objects;

/**
 * Command to create the technician profile of a newly registered platform account.
 *
 * <p>Raised by the Anti-Corruption Layer once the values received from the registering context
 * have been translated into profiles domain value objects.</p>
 *
 * @param profileCreationData the validated shared creation data; required
 * @param qualification       the technician's speciality and certification; required
 */
public record CreateTechnicianProfileCommand(ProfileCreationData profileCreationData, TechnicianQualification qualification) {

  /**
   * Validates that both components were supplied.
   */
  public CreateTechnicianProfileCommand {
    Objects.requireNonNull(profileCreationData, "profileCreationData must not be null");
    Objects.requireNonNull(qualification, "qualification must not be null");
  }
}
