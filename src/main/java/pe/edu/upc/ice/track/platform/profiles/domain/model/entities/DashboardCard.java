package pe.edu.upc.ice.track.platform.profiles.domain.model.entities;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardType;

import java.util.Objects;

/**
 * Dashboard card entity.
 *
 * <p>A widget shown on a user's dashboard. It is an internal entity of the
 * {@link DashboardConfig} aggregate: it has no repository of its own, and is only ever created,
 * changed or removed through its dashboard configuration.</p>
 *
 * <p>No JPA or persistence annotation is present here - those concerns live exclusively in
 * {@code DashboardCardPersistenceEntity}.</p>
 */
@Getter
public class DashboardCard {

  private final Long cardId;
  private final CardType cardType;
  private Integer order;
  private boolean visible;

  /**
   * Creates a new, not yet persisted, card.
   *
   * @param cardType the kind of widget; required
   * @param order    the position of the card on the dashboard, zero or greater; required
   * @param visible  whether the card is shown
   */
  public DashboardCard(CardType cardType, Integer order, boolean visible) {
    this(null, cardType, order, visible);
  }

  /**
   * Reconstitutes a card.
   *
   * @param cardId   the persistence identity, or {@code null} for a card not yet persisted
   * @param cardType the kind of widget; required
   * @param order    the position of the card on the dashboard, zero or greater; required
   * @param visible  whether the card is shown
   */
  public DashboardCard(Long cardId, CardType cardType, Integer order, boolean visible) {
    this.cardId = cardId;
    this.cardType = Objects.requireNonNull(cardType, "cardType must not be null");
    this.order = requireValidOrder(order);
    this.visible = visible;
  }

  /**
   * Shows the card when hidden, hides it when shown.
   */
  public void changeVisibility() {
    this.visible = !this.visible;
  }

  /**
   * Moves the card to another position on the dashboard.
   *
   * @param newOrder the new position, zero or greater; required
   */
  public void updateOrder(Integer newOrder) {
    this.order = requireValidOrder(newOrder);
  }

  private static Integer requireValidOrder(Integer order) {
    if (order == null) {
      throw new IllegalArgumentException("Card order must not be null");
    }
    if (order < 0) {
      throw new IllegalArgumentException("Card order must not be negative");
    }
    return order;
  }
}
