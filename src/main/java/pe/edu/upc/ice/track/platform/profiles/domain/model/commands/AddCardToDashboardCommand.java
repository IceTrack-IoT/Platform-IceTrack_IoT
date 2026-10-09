package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardType;

import java.util.Objects;

/**
 * Command to append a card to the dashboard of a platform account.
 *
 * <p>Carries no position: the dashboard configuration places the new card after the existing
 * ones.</p>
 *
 * @param userId    identifier of the account whose dashboard receives the card; required
 * @param cardType  the kind of widget; required
 * @param isVisible whether the card is shown
 */
public record AddCardToDashboardCommand(Long userId, CardType cardType, boolean isVisible) {

  /**
   * Validates that every mandatory component was supplied.
   */
  public AddCardToDashboardCommand {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(cardType, "cardType must not be null");
  }
}
