package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import pe.edu.upc.ice.track.platform.profiles.domain.model.factories.ProfileCreationData;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Objects;

/**
 * Command to create the technician profile of a newly registered platform account.
 *
 * <p>Raised by the Anti-Corruption Layer once the values received from the registering context
 * have been translated into profiles domain value objects.</p>
 *
 * @param userId identifier of the account the technician belongs to; required
 * @param data   the attributes of the technician profile, including its speciality and
 *               certification number; required
 */
public record CreateTechnicianCommand(UserId userId, ProfileCreationData data) {

  /**
   * Validates that every component was supplied.
   */
  public CreateTechnicianCommand {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(data, "data must not be null");
    Objects.requireNonNull(data.speciality(), "speciality must not be null");
    if (data.certificationNumber() == null || data.certificationNumber().isBlank()) {
      throw new IllegalArgumentException("Certification number must not be null or blank");
    }
  }
}
