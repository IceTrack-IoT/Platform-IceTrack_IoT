package pe.edu.upc.ice.track.platform.profiles.domain.model.entities;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardType;

import java.util.Objects;

/**
 * Dashboard card entity.
 *
 * <p>A widget shown on a user's dashboard. It is an internal entity of the
 * {@link DashboardConfig} aggregate: it has no repository of its own, is provisioned together with
 * its dashboard configuration and is never deleted - hiding a widget only clears its visibility, so
 * a hidden card keeps its data.</p>
 *
 * <p>No JPA or persistence annotation is present here - those concerns live exclusively in
 * {@code DashboardCardPersistenceEntity}.</p>
 */
@Getter
public class DashboardCard {

  private final Long cardId;
  private DashboardConfig dashboardConfig;
  private final CardType cardType;
  private Integer order;
  private boolean visible;

  /**
   * Creates a new, not yet persisted, card.
   *
   * <p>Reserved to {@link DashboardConfig}, which provisions one card per {@link CardType} and
   * computes its position: a card's order is never chosen on its own.</p>
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
   * Associates the card with the dashboard configuration that owns it.
   *
   * <p>Reserved to {@link DashboardConfig}, which associates every card it holds. A card belongs to
   * a single configuration for its whole life: it can not be moved to another one.</p>
   *
   * @param dashboardConfig the owning dashboard configuration; required
   * @throws IllegalStateException when the card already belongs to another configuration
   */
  public void setDashboardConfig(DashboardConfig dashboardConfig) {
    Objects.requireNonNull(dashboardConfig, "dashboardConfig must not be null");
    if (this.dashboardConfig != null && this.dashboardConfig != dashboardConfig) {
      throw new IllegalStateException("Card already belongs to another dashboard configuration");
    }
    this.dashboardConfig = dashboardConfig;
  }

  /**
   * Shows or hides the card. A hidden card keeps its data.
   *
   * <p>Reserved to {@link DashboardConfig}.</p>
   *
   * @param visible whether the card is shown
   */
  public void setVisibility(boolean visible) {
    this.visible = visible;
  }

  /**
   * Shows the card when hidden, hides it when shown.
   *
   * <p>Reserved to {@link DashboardConfig#toggleCardVisibility}.</p>
   */
  public void changeVisibility() {
    setVisibility(!this.visible);
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
