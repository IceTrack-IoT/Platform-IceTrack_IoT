package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import java.util.Objects;

/**
 * Command to remove a card from the dashboard of a platform account.
 *
 * @param userId identifier of the account owning the dashboard; required
 * @param cardId identifier of the card; required
 */
public record RemoveCardFromDashboardCommand(Long userId, Long cardId) {

  /**
   * Validates that every component was supplied.
   */
  public RemoveCardFromDashboardCommand {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(cardId, "cardId must not be null");
  }
}
