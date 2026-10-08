package pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates;

import lombok.AccessLevel;
import lombok.Getter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.entities.DashboardCard;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardType;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TemperatureRange;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Dashboard configuration aggregate root.
 *
 * <p>The dashboard preferences of a platform account: the site and temperature range the
 * dashboard opens on, and the {@link DashboardCard cards} it shows. An account has at most one
 * configuration.</p>
 *
 * <p>The cards are internal entities of this aggregate. They are only reachable through
 * {@link #getCards()}, a read-only view, and every change to them - adding, toggling, removing -
 * goes through this root, which guards the aggregate invariants:</p>
 * <ul>
 *   <li>a dashboard never shows the same {@link CardType} twice;</li>
 *   <li>the cards are always kept sorted by their order.</li>
 * </ul>
 *
 * <p>No JPA or persistence annotation is present here - those concerns live exclusively in
 * {@code DashboardConfigPersistenceEntity}.</p>
 */
@Getter
public class DashboardConfig extends AbstractDomainAggregateRoot<DashboardConfig> {

  private static final Comparator<DashboardCard> BY_ORDER = Comparator.comparing(DashboardCard::getOrder);

  private final Long dashboardConfigId;
  private final UserId userId;
  private SiteId defaultSiteId;
  private TemperatureRange defaultTemperatureRange;
  @Getter(AccessLevel.NONE)
  private final List<DashboardCard> cards;

  /**
   * Creates a new, not yet persisted, dashboard configuration with no cards.
   *
   * @param userId                  identifier of the account the configuration belongs to; required
   * @param defaultSiteId           the site the dashboard opens on; required
   * @param defaultTemperatureRange the temperature range the dashboard opens on; required
   */
  public DashboardConfig(UserId userId, SiteId defaultSiteId, TemperatureRange defaultTemperatureRange) {
    this(null, userId, defaultSiteId, defaultTemperatureRange, List.of());
  }

  /**
   * Reconstitutes a dashboard configuration.
   *
   * @param dashboardConfigId       the persistence identity, or {@code null} for a configuration not yet persisted
   * @param userId                  identifier of the account the configuration belongs to; required
   * @param defaultSiteId           the site the dashboard opens on; required
   * @param defaultTemperatureRange the temperature range the dashboard opens on; required
   * @param cards                   the cards of the dashboard; required, may be empty
   */
  public DashboardConfig(Long dashboardConfigId, UserId userId, SiteId defaultSiteId,
                         TemperatureRange defaultTemperatureRange, Collection<DashboardCard> cards) {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(userId.userId(), "userId must carry an identifier");
    Objects.requireNonNull(cards, "cards must not be null");
    this.dashboardConfigId = dashboardConfigId;
    this.userId = userId;
    this.defaultSiteId = requireSiteId(defaultSiteId);
    this.defaultTemperatureRange = Objects.requireNonNull(defaultTemperatureRange, "defaultTemperatureRange must not be null");
    this.cards = new ArrayList<>();
    cards.forEach(this::appendCard);
  }

  /**
   * Returns the cards of the dashboard, sorted by their order.
   *
   * @return a read-only view of the cards; changes must go through this aggregate root
   */
  public List<DashboardCard> getCards() {
    return Collections.unmodifiableList(cards);
  }

  /**
   * Finds a card of this dashboard.
   *
   * @param cardId the card identifier
   * @return the card, or empty when this dashboard has no such card
   */
  public Optional<DashboardCard> findCard(Long cardId) {
    if (cardId == null) return Optional.empty();
    return cards.stream().filter(card -> cardId.equals(card.getCardId())).findFirst();
  }

  /**
   * Checks whether this dashboard already shows a card of the given type.
   *
   * @param cardType the card type
   * @return true when a card of that type is on the dashboard
   */
  public boolean hasCardOfType(CardType cardType) {
    return cards.stream().anyMatch(card -> card.getCardType() == cardType);
  }

  /**
   * Adds a new card to the dashboard.
   *
   * @param cardType the kind of widget; required, and not yet on the dashboard
   * @param order    the position of the card, zero or greater; required
   * @param visible  whether the card is shown
   * @return the added card, not yet persisted
   * @throws IllegalStateException when the dashboard already shows a card of that type
   */
  public DashboardCard addCard(CardType cardType, Integer order, boolean visible) {
    var card = new DashboardCard(cardType, order, visible);
    appendCard(card);
    return card;
  }

  /**
   * Shows a hidden card of this dashboard, or hides a shown one.
   *
   * @param cardId the card identifier
   * @throws IllegalArgumentException when this dashboard has no such card
   */
  public void toggleCardVisibility(Long cardId) {
    findCard(cardId)
        .orElseThrow(() -> new IllegalArgumentException("Dashboard has no card %s".formatted(cardId)))
        .changeVisibility();
  }

  /**
   * Moves a card of this dashboard to another position.
   *
   * @param cardId   the card identifier
   * @param newOrder the new position, zero or greater; required
   * @throws IllegalArgumentException when this dashboard has no such card or the order is invalid
   */
  public void reorderCard(Long cardId, Integer newOrder) {
    findCard(cardId)
        .orElseThrow(() -> new IllegalArgumentException("Dashboard has no card %s".formatted(cardId)))
        .updateOrder(newOrder);
    cards.sort(BY_ORDER);
  }

  /**
   * Removes a card from the dashboard. Once saved, the card no longer exists.
   *
   * @param cardId the card identifier
   * @throws IllegalArgumentException when this dashboard has no such card
   */
  public void removeCard(Long cardId) {
    if (cardId == null || !cards.removeIf(card -> cardId.equals(card.getCardId()))) {
      throw new IllegalArgumentException("Dashboard has no card %s".formatted(cardId));
    }
  }

  /**
   * Replaces the site and temperature range the dashboard opens on.
   *
   * @param defaultSiteId           the new default site; required
   * @param defaultTemperatureRange the new default temperature range; required
   */
  public void updateDefaults(SiteId defaultSiteId, TemperatureRange defaultTemperatureRange) {
    this.defaultSiteId = requireSiteId(defaultSiteId);
    this.defaultTemperatureRange = Objects.requireNonNull(defaultTemperatureRange, "defaultTemperatureRange must not be null");
  }

  private static SiteId requireSiteId(SiteId siteId) {
    Objects.requireNonNull(siteId, "defaultSiteId must not be null");
    Objects.requireNonNull(siteId.siteId(), "defaultSiteId must carry an identifier");
    return siteId;
  }

  private void appendCard(DashboardCard card) {
    Objects.requireNonNull(card, "card must not be null");
    if (hasCardOfType(card.getCardType())) {
      throw new IllegalStateException("Dashboard already shows a %s card".formatted(card.getCardType()));
    }
    cards.add(card);
    cards.sort(BY_ORDER);
  }
}
