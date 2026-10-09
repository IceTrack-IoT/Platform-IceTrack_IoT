package pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates;

import lombok.AccessLevel;
import lombok.Getter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.entities.DashboardCard;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardLayoutItem;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardType;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TemperatureRange;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Dashboard configuration aggregate root.
 *
 * <p>The dashboard preferences of a platform account: the site and temperature range the
 * dashboard opens on, and the layout of its {@link DashboardCard cards}. An account has at most one
 * configuration.</p>
 *
 * <p>The cards are internal entities of this aggregate. They are only reachable through
 * {@link #getCards()}, a read-only view, and every change to them - showing, hiding, reordering,
 * resetting - goes through this root, which guards the aggregate invariants:</p>
 * <ul>
 *   <li>the dashboard holds exactly one card of each {@link CardType}: they are provisioned with
 *   the configuration, and never added or removed afterwards - hiding a card only clears its
 *   visibility, so a hidden card keeps its data;</li>
 *   <li>the card order is the contiguous, 1-based sequence {@code 1, 2, ..., N}, with no gap and
 *   no position shared by two cards;</li>
 *   <li>the cards are always kept sorted by their order.</li>
 * </ul>
 *
 * <p>No JPA or persistence annotation is present here - those concerns live exclusively in
 * {@code DashboardConfigPersistenceEntity}.</p>
 */
@Getter
public class DashboardConfig extends AbstractDomainAggregateRoot<DashboardConfig> {

  /**
   * Visibility of every card in the default layout.
   */
  private static final boolean DEFAULT_VISIBILITY = true;

  private static final Comparator<DashboardCard> BY_ORDER = Comparator.comparing(DashboardCard::getOrder);

  /**
   * The default layout sequence: the declaration order of {@link CardType}.
   */
  private static final Comparator<DashboardCard> BY_DEFAULT_LAYOUT = Comparator.comparing(DashboardCard::getCardType);

  private final Long dashboardConfigId;
  private final UserId userId;
  private SiteId defaultSiteId;
  private TemperatureRange defaultTemperatureRange;
  @Getter(AccessLevel.NONE)
  private final List<DashboardCard> cards;

  /**
   * Creates a new, not yet persisted, dashboard configuration in the default layout: one visible
   * card of each {@link CardType}, at positions {@code 1..N} in the declaration order of the type.
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
   * <p>The stored cards are sorted by their stored order; a card type with no stored card - a
   * configuration saved before every type existed - is provisioned after them, visible. The cards
   * are then renumbered {@code 1..N}, so the invariants hold even for rows stored with gaps or
   * duplicate positions; the repaired layout is written back the next time the configuration is
   * saved.</p>
   *
   * @param dashboardConfigId       the persistence identity, or {@code null} for a configuration not yet persisted
   * @param userId                  identifier of the account the configuration belongs to; required
   * @param defaultSiteId           the site the dashboard opens on; required
   * @param defaultTemperatureRange the temperature range the dashboard opens on; required
   * @param cards                   the stored cards of the dashboard, at most one per type; required, may be empty
   * @throws IllegalStateException when two stored cards share a type
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
    this.cards.sort(BY_ORDER);
    provisionMissingCards();
    reindexCards();
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
   * Shows or hides a card of this dashboard. A hidden card keeps its data and its position.
   *
   * @param cardId    the card identifier
   * @param isVisible whether the card is shown
   * @throws IllegalArgumentException when this dashboard has no such card
   */
  public void updateCardVisibility(Long cardId, boolean isVisible) {
    requireCard(cardId).setVisibility(isVisible);
  }

  /**
   * Shows a hidden card of this dashboard, or hides a shown one.
   *
   * @param cardId the card identifier
   * @throws IllegalArgumentException when this dashboard has no such card
   */
  public void toggleCardVisibility(Long cardId) {
    requireCard(cardId).changeVisibility();
  }

  /**
   * Replaces the whole card layout: the position and visibility of every card.
   *
   * <p>The layout is checked as a whole before any card changes, so an invalid layout leaves the
   * dashboard untouched. It must place every card of this dashboard exactly once, at the positions
   * {@code 1..N}: a missing, unknown or repeated card, and a missing, out of range or repeated
   * position, are all rejected - which also rules out any gap.</p>
   *
   * @param layout the requested placement of every card; required
   * @throws IllegalArgumentException when the layout does not place every card exactly once at the positions {@code 1..N}
   */
  public void updateLayout(List<CardLayoutItem> layout) {
    if (layout == null) {
      throw new IllegalArgumentException("Layout must not be null");
    }
    if (layout.size() != cards.size()) {
      throw new IllegalArgumentException("Layout must place exactly %d cards, %d were given"
          .formatted(cards.size(), layout.size()));
    }
    var placements = new HashMap<DashboardCard, CardLayoutItem>();
    var takenOrders = new HashSet<Integer>();
    for (var item : layout) {
      if (item == null) {
        throw new IllegalArgumentException("Layout entries must not be null");
      }
      var card = requireCard(item.cardId());
      if (placements.putIfAbsent(card, item) != null) {
        throw new IllegalArgumentException("Card %s is placed more than once".formatted(item.cardId()));
      }
      if (item.order() < 1 || item.order() > cards.size()) {
        throw new IllegalArgumentException("Card order must be between 1 and %d, %d was given"
            .formatted(cards.size(), item.order()));
      }
      if (!takenOrders.add(item.order())) {
        throw new IllegalArgumentException("Order %d is given to more than one card".formatted(item.order()));
      }
    }
    // N distinct, existing cards at N distinct positions within 1..N: every card is placed, and the positions are exactly 1..N
    placements.forEach((card, item) -> {
      card.updateOrder(item.order());
      card.setVisibility(item.visible());
    });
    cards.sort(BY_ORDER);
  }

  /**
   * Restores the default layout: every card visible, at positions {@code 1..N} in the declaration
   * order of {@link CardType}. The site and temperature range defaults are left unchanged.
   */
  public void resetToDefaults() {
    cards.sort(BY_DEFAULT_LAYOUT);
    cards.forEach(card -> card.setVisibility(DEFAULT_VISIBILITY));
    reindexCards();
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

  private DashboardCard requireCard(Long cardId) {
    return findCard(cardId)
        .orElseThrow(() -> new IllegalArgumentException("Dashboard has no card %s".formatted(cardId)));
  }

  private boolean hasCardOfType(CardType cardType) {
    return cards.stream().anyMatch(card -> card.getCardType() == cardType);
  }

  /**
   * Appends, visible and after the existing cards, a card of every type the dashboard does not
   * hold yet, in the declaration order of {@link CardType}.
   */
  private void provisionMissingCards() {
    for (var cardType : CardType.values()) {
      if (!hasCardOfType(cardType)) {
        appendCard(new DashboardCard(cardType, cards.size() + 1, DEFAULT_VISIBILITY));
      }
    }
  }

  private void appendCard(DashboardCard card) {
    Objects.requireNonNull(card, "card must not be null");
    if (hasCardOfType(card.getCardType())) {
      throw new IllegalStateException("Dashboard already holds a %s card".formatted(card.getCardType()));
    }
    card.setDashboardConfig(this);
    cards.add(card);
  }

  /**
   * Renumbers the cards {@code 1..N} following their current sequence, leaving no gap.
   */
  private void reindexCards() {
    for (int i = 0; i < cards.size(); i++) {
      cards.get(i).updateOrder(i + 1);
    }
  }
}
