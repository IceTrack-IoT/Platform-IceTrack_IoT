package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

/**
 * Card Layout Item Value Object.
 *
 * <p>The requested placement of one card in a dashboard layout: its position and whether it is
 * shown. A layout is only meaningful as a whole - it is {@code DashboardConfig#updateLayout} that
 * checks every card of the dashboard is placed exactly once, at positions {@code 1..N}.</p>
 *
 * @param cardId  identifier of the card; required
 * @param order   the requested 1-based position of the card; required
 * @param visible whether the card is shown
 */
public record CardLayoutItem(Long cardId, Integer order, boolean visible) {

  /**
   * Validates that every mandatory component was supplied.
   *
   * @throws IllegalArgumentException when the card identifier or the order is missing
   */
  public CardLayoutItem {
    if (cardId == null) {
      throw new IllegalArgumentException("Layout card identifier must not be null");
    }
    if (order == null) {
      throw new IllegalArgumentException("Layout card order must not be null");
    }
  }
}
