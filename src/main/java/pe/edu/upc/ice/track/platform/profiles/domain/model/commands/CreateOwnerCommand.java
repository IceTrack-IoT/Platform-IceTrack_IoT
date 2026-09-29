package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import pe.edu.upc.ice.track.platform.profiles.domain.model.factories.ProfileCreationData;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Objects;

/**
 * Command to create the owner profile of a newly registered platform account.
 *
 * <p>Raised by the Anti-Corruption Layer once the values received from the registering context
 * have been translated into profiles domain value objects.</p>
 *
 * @param userId identifier of the account the owner belongs to; required
 * @param data   the attributes of the owner profile, including its RUC; required
 */
public record CreateOwnerCommand(UserId userId, ProfileCreationData data) {

  /**
   * Validates that every component was supplied.
   */
  public CreateOwnerCommand {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(data, "data must not be null");
    Objects.requireNonNull(data.ruc(), "ruc must not be null");
  }
}
