package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import java.util.Objects;

/**
 * Placement of one card within an {@link UpdateDashboardLayoutCommand}.
 *
 * @param cardId    identifier of the card; required
 * @param order     the requested 1-based position of the card; required
 * @param isVisible whether the card is shown
 */
public record CardLayoutCommandItem(Long cardId, Integer order, boolean isVisible) {

  /**
   * Validates that every mandatory component was supplied.
   */
  public CardLayoutCommandItem {
    Objects.requireNonNull(cardId, "cardId must not be null");
    Objects.requireNonNull(order, "order must not be null");
  }
}
