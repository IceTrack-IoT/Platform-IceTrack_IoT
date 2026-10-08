package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardType;

import java.util.Objects;

/**
 * Command to add a card to the dashboard of a platform account.
 *
 * @param userId    identifier of the account whose dashboard receives the card; required
 * @param cardType  the kind of widget; required
 * @param order     the position of the card, zero or greater; required
 * @param isVisible whether the card is shown
 */
public record AddCardToDashboardCommand(Long userId, CardType cardType, Integer order, boolean isVisible) {

  /**
   * Validates that every mandatory component was supplied.
   */
  public AddCardToDashboardCommand {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(cardType, "cardType must not be null");
    Objects.requireNonNull(order, "order must not be null");
  }
}
