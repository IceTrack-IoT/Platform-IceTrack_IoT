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
   * <p>Reserved to {@link DashboardConfig#addCard}, which computes the position: a card's order is
   * never chosen by a caller.</p>
   *
   * @param cardType the kind of widget; required
   * @param order    the 1-based position of the card on the dashboard; required
   * @param visible  whether the card is shown
   */
  public DashboardCard(CardType cardType, Integer order, boolean visible) {
    this(null, cardType, requireValidOrder(order), visible);
  }

  /**
   * Reconstitutes a card.
   *
   * <p>The stored order is only required to be present, not to be valid: the owning
   * {@link DashboardConfig} renumbers its cards {@code 1..N} on reconstitution, which also repairs
   * positions stored by an earlier scheme.</p>
   *
   * @param cardId   the persistence identity, or {@code null} for a card not yet persisted
   * @param cardType the kind of widget; required
   * @param order    the stored position of the card on the dashboard; required
   * @param visible  whether the card is shown
   */
  public DashboardCard(Long cardId, CardType cardType, Integer order, boolean visible) {
    if (order == null) {
      throw new IllegalArgumentException("Card order must not be null");
    }
    this.cardId = cardId;
    this.cardType = Objects.requireNonNull(cardType, "cardType must not be null");
    this.order = order;
    this.visible = visible;
  }

  /**
   * Shows the card when hidden, hides it when shown.
   *
   * <p>Reserved to {@link DashboardConfig#toggleCardVisibility}.</p>
   */
  public void changeVisibility() {
    this.visible = !this.visible;
  }

  /**
   * Moves the card to another position on the dashboard.
   *
   * <p>Reserved to {@link DashboardConfig}, which keeps the order of all its cards contiguous:
   * calling it from anywhere else can leave gaps or duplicate positions.</p>
   *
   * @param newOrder the new 1-based position; required
   */
  public void updateOrder(Integer newOrder) {
    this.order = requireValidOrder(newOrder);
  }

  private static Integer requireValidOrder(Integer order) {
    if (order == null) {
      throw new IllegalArgumentException("Card order must not be null");
    }
    if (order < 1) {
      throw new IllegalArgumentException("Card order must be 1 or greater");
    }
    return order;
  }
}
