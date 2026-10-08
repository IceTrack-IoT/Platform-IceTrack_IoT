package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import java.util.Objects;

/**
 * Command to show a hidden dashboard card, or hide a shown one.
 *
 * @param userId identifier of the account owning the dashboard; required
 * @param cardId identifier of the card; required
 */
public record ToggleCardVisibilityCommand(Long userId, Long cardId) {

  /**
   * Validates that every component was supplied.
   */
  public ToggleCardVisibilityCommand {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(cardId, "cardId must not be null");
  }
}
