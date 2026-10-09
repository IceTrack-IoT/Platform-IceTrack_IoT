package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import java.util.List;
import java.util.Objects;

/**
 * Command to replace the card layout - the position and visibility of every card - of the
 * dashboard of a platform account.
 *
 * @param userId identifier of the account owning the dashboard; required
 * @param cards  the placement of every card of the dashboard; required, with no null entry
 */
public record UpdateDashboardLayoutCommand(Long userId, List<CardLayoutCommandItem> cards) {

  /**
   * Validates that every component was supplied, and takes an immutable copy of the cards.
   */
  public UpdateDashboardLayoutCommand {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(cards, "cards must not be null");
    cards = List.copyOf(cards);
  }
}
