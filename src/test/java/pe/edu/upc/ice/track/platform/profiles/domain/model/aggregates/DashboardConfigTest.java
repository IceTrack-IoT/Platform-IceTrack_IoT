package pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.profiles.domain.model.entities.DashboardCard;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardLayoutItem;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.CardType;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TemperatureRange;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.DASHBOARD_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.DASHBOARD_VERSION;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.SITE_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.dashboard;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedCards;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedDashboard;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.temperatureRange;

class DashboardConfigTest {

  private static List<CardType> typesInOrder(DashboardConfig config) {
    return config.getCards().stream().map(DashboardCard::getCardType).toList();
  }

  private static List<CardLayoutItem> layout(Object[][] rows) {
    var items = new ArrayList<CardLayoutItem>();
    for (var row : rows) {
      items.add(new CardLayoutItem((Long) row[0], (Integer) row[1], (Boolean) row[2]));
    }
    return items;
  }

  @Nested
  class Construction {

    @Test
    @DisplayName("new DashboardConfig(user, site, range) -> provisions the four card types, visible, ordered 1..4 (AAA)")
    void provisionsAllCards() {
      // Act
      var config = dashboard();

      // Assert
      assertThat(config.getDashboardConfigId()).isNull();
      assertThat(config.getVersion()).isNull();
      assertThat(typesInOrder(config)).containsExactly(CardType.values());
      assertThat(config.getCards()).extracting(DashboardCard::getOrder).containsExactly(1, 2, 3, 4);
      assertThat(config.getCards()).allMatch(DashboardCard::isVisible);
      assertThat(config.getCards()).allMatch(card -> card.getDashboardConfig() == config);
      assertThat(config.getDefaultSiteId()).isEqualTo(new SiteId(SITE_ID));
      assertThat(config.getDefaultTemperatureRange()).isEqualTo(temperatureRange());
    }

    @Test
    @DisplayName("new DashboardConfig(partial cards) -> keeps theirs, appends the missing ones and reindexes (AAA)")
    void completesMissingCards() {
      // Arrange
      var existing = List.of(new DashboardCard(7L, CardType.OPEN_ALERTS, 5, false));

      // Act
      var config = new DashboardConfig(DASHBOARD_ID, new UserId(USER_ID), new SiteId(SITE_ID), temperatureRange(),
          existing);

      // Assert
      assertThat(config.getCards()).hasSize(4);
      assertThat(config.getCards().get(0).getCardType()).isEqualTo(CardType.OPEN_ALERTS);
      assertThat(config.getCards().get(0).isVisible()).isFalse();
      assertThat(config.getCards()).extracting(DashboardCard::getOrder).containsExactly(1, 2, 3, 4);
      assertThat(config.getCards().subList(1, 4)).allMatch(DashboardCard::isVisible);
    }

    @Test
    @DisplayName("new DashboardConfig(cards in shuffled order) -> sorted by their stored order (AAA)")
    void sortsStoredCards() {
      // Arrange
      var cards = List.of(
          new DashboardCard(1L, CardType.EQUIPMENT_STATUS, 1, true),
          new DashboardCard(2L, CardType.MONITORED_EQUIPMENT, 2, true),
          new DashboardCard(3L, CardType.ACTIVE_ORDERS, 3, true),
          new DashboardCard(4L, CardType.OPEN_ALERTS, 4, true));

      // Act
      var config = new DashboardConfig(DASHBOARD_ID, DASHBOARD_VERSION, new UserId(USER_ID), new SiteId(SITE_ID),
          temperatureRange(), cards);

      // Assert
      assertThat(typesInOrder(config)).containsExactly(CardType.EQUIPMENT_STATUS, CardType.MONITORED_EQUIPMENT,
          CardType.ACTIVE_ORDERS, CardType.OPEN_ALERTS);
      assertThat(config.getVersion()).isEqualTo(DASHBOARD_VERSION);
      assertThat(config.getDashboardConfigId()).isEqualTo(DASHBOARD_ID);
    }

    @Test
    @DisplayName("new DashboardConfig(two cards of the same type) -> IllegalStateException (AAA)")
    void duplicateCardTypeRejected() {
      // Arrange
      var cards = List.of(
          new DashboardCard(CardType.OPEN_ALERTS, 1, true),
          new DashboardCard(CardType.OPEN_ALERTS, 2, true));

      // Act & Assert
      assertThatThrownBy(() -> new DashboardConfig(DASHBOARD_ID, new UserId(USER_ID), new SiteId(SITE_ID),
          temperatureRange(), cards))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Dashboard already holds a OPEN_ALERTS card");
    }

    @Test
    @DisplayName("new DashboardConfig(invalid arguments) -> NullPointerException with the missing name (AAA)")
    void invalidArguments() {
      // Arrange
      var user = new UserId(USER_ID);
      var site = new SiteId(SITE_ID);
      var range = temperatureRange();

      // Act & Assert
      assertThatThrownBy(() -> new DashboardConfig(null, site, range)).hasMessage("userId must not be null");
      assertThatThrownBy(() -> new UserId(0L)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new DashboardConfig(user, null, range)).hasMessage("defaultSiteId must not be null");
      assertThatThrownBy(() -> new SiteId(null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new DashboardConfig(user, site, null))
          .hasMessage("defaultTemperatureRange must not be null");
      assertThatThrownBy(() -> new DashboardConfig(1L, user, site, range, null)).hasMessage("cards must not be null");
    }

    @Test
    @DisplayName("getCards() -> unmodifiable view (AAA)")
    void cardsAreUnmodifiable() {
      // Arrange
      var config = dashboard();

      // Act & Assert
      assertThatThrownBy(() -> config.getCards().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  class CardLookupAndVisibility {

    @Test
    @DisplayName("findCard(id) -> the card when present, empty for unknown or null ids (AAA)")
    void findCard() {
      // Arrange
      var config = persistedDashboard();

      // Act & Assert
      assertThat(config.findCard(102L)).get().extracting(DashboardCard::getCardType).isEqualTo(CardType.OPEN_ALERTS);
      assertThat(config.findCard(999L)).isEmpty();
      assertThat(config.findCard(null)).isEmpty();
    }

    @Test
    @DisplayName("updateCardVisibility(id, false) -> hides only that card (AAA)")
    void updateVisibility() {
      // Arrange
      var config = persistedDashboard();

      // Act
      config.updateCardVisibility(103L, false);

      // Assert
      assertThat(config.findCard(103L).orElseThrow().isVisible()).isFalse();
      assertThat(config.findCard(101L).orElseThrow().isVisible()).isTrue();
    }

    @Test
    @DisplayName("toggleCardVisibility(id) twice -> back to the original visibility (AAA)")
    void toggleTwice() {
      // Arrange
      var config = persistedDashboard();

      // Act
      config.toggleCardVisibility(101L);
      var afterFirst = config.findCard(101L).orElseThrow().isVisible();
      config.toggleCardVisibility(101L);

      // Assert
      assertThat(afterFirst).isFalse();
      assertThat(config.findCard(101L).orElseThrow().isVisible()).isTrue();
    }

    @Test
    @DisplayName("toggleCardVisibility(unknown id) -> IllegalArgumentException (AAA)")
    void toggleUnknown() {
      // Arrange
      var config = persistedDashboard();

      // Act & Assert
      assertThatThrownBy(() -> config.toggleCardVisibility(999L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Dashboard has no card 999");
      assertThatThrownBy(() -> config.updateCardVisibility(null, true)).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class UpdateLayout {

    @Test
    @DisplayName("updateLayout(permutation) -> applies order and visibility and sorts the cards (AAA)")
    void appliesLayout() {
      // Arrange
      var config = persistedDashboard();

      // Act
      config.updateLayout(layout(new Object[][] {{101L, 4, false}, {102L, 3, true}, {103L, 2, true}, {104L, 1, true}}));

      // Assert
      assertThat(typesInOrder(config)).containsExactly(CardType.EQUIPMENT_STATUS, CardType.ACTIVE_ORDERS,
          CardType.OPEN_ALERTS, CardType.MONITORED_EQUIPMENT);
      assertThat(config.findCard(101L).orElseThrow().isVisible()).isFalse();
      assertThat(config.findCard(101L).orElseThrow().getOrder()).isEqualTo(4);
    }

    @Test
    @DisplayName("updateLayout(null) -> IllegalArgumentException (AAA)")
    void nullLayout() {
      // Act & Assert
      assertThatThrownBy(() -> persistedDashboard().updateLayout(null))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("Layout must not be null");
    }

    @Test
    @DisplayName("updateLayout(wrong size) -> IllegalArgumentException stating expected and given (AAA)")
    void wrongSize() {
      // Act & Assert
      assertThatThrownBy(() -> persistedDashboard().updateLayout(layout(new Object[][] {{101L, 1, true}})))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Layout must place exactly 4 cards, 1 were given");
    }

    @Test
    @DisplayName("updateLayout(unknown card) -> IllegalArgumentException (AAA)")
    void unknownCard() {
      // Act & Assert
      assertThatThrownBy(() -> persistedDashboard().updateLayout(
          layout(new Object[][] {{101L, 1, true}, {102L, 2, true}, {103L, 3, true}, {999L, 4, true}})))
          .hasMessage("Dashboard has no card 999");
    }

    @Test
    @DisplayName("updateLayout(card placed twice) -> IllegalArgumentException (AAA)")
    void cardPlacedTwice() {
      // Act & Assert
      assertThatThrownBy(() -> persistedDashboard().updateLayout(
          layout(new Object[][] {{101L, 1, true}, {101L, 2, true}, {103L, 3, true}, {104L, 4, true}})))
          .hasMessage("Card 101 is placed more than once");
    }

    @Test
    @DisplayName("updateLayout(order out of range) -> IllegalArgumentException (AAA)")
    void orderOutOfRange() {
      // Act & Assert
      assertThatThrownBy(() -> persistedDashboard().updateLayout(
          layout(new Object[][] {{101L, 0, true}, {102L, 2, true}, {103L, 3, true}, {104L, 4, true}})))
          .hasMessage("Card order must be between 1 and 4, 0 was given");
      assertThatThrownBy(() -> persistedDashboard().updateLayout(
          layout(new Object[][] {{101L, 5, true}, {102L, 2, true}, {103L, 3, true}, {104L, 4, true}})))
          .hasMessage("Card order must be between 1 and 4, 5 was given");
    }

    @Test
    @DisplayName("updateLayout(repeated order) -> IllegalArgumentException (AAA)")
    void repeatedOrder() {
      // Act & Assert
      assertThatThrownBy(() -> persistedDashboard().updateLayout(
          layout(new Object[][] {{101L, 1, true}, {102L, 1, true}, {103L, 3, true}, {104L, 4, true}})))
          .hasMessage("Order 1 is given to more than one card");
    }

    @Test
    @DisplayName("updateLayout(null entry) -> IllegalArgumentException (AAA)")
    void nullEntry() {
      // Arrange
      var items = new ArrayList<CardLayoutItem>(layout(new Object[][] {{101L, 1, true}, {102L, 2, true}, {103L, 3, true}}));
      items.add(null);

      // Act & Assert
      assertThatThrownBy(() -> persistedDashboard().updateLayout(items))
          .hasMessage("Layout entries must not be null");
    }

    @Test
    @DisplayName("updateLayout(invalid) -> no card is modified (AAA)")
    void invalidLayoutLeavesCardsUntouched() {
      // Arrange
      var config = persistedDashboard();

      // Act
      assertThatThrownBy(() -> config.updateLayout(
          layout(new Object[][] {{101L, 4, false}, {102L, 3, true}, {103L, 2, true}, {999L, 1, true}})))
          .isInstanceOf(IllegalArgumentException.class);

      // Assert
      assertThat(config.getCards()).extracting(DashboardCard::getOrder).containsExactly(1, 2, 3, 4);
      assertThat(config.getCards()).allMatch(DashboardCard::isVisible);
    }
  }

  @Nested
  class ResetAndDefaults {

    @Test
    @DisplayName("resetToDefaults() -> card-type order, all visible, reindexed (AAA)")
    void resetToDefaults() {
      // Arrange
      var config = persistedDashboard();
      config.updateLayout(layout(new Object[][] {{101L, 4, false}, {102L, 3, false}, {103L, 2, true}, {104L, 1, true}}));

      // Act
      config.resetToDefaults();

      // Assert
      assertThat(typesInOrder(config)).containsExactly(CardType.values());
      assertThat(config.getCards()).extracting(DashboardCard::getOrder).containsExactly(1, 2, 3, 4);
      assertThat(config.getCards()).allMatch(DashboardCard::isVisible);
    }

    @Test
    @DisplayName("updateDefaults(site, range) -> replaces both (AAA)")
    void updateDefaults() {
      // Arrange
      var config = dashboard();
      var range = new TemperatureRange(-20, -15, "C", "Freezer");

      // Act
      config.updateDefaults(new SiteId(4L), range);

      // Assert
      assertThat(config.getDefaultSiteId().siteId()).isEqualTo(4L);
      assertThat(config.getDefaultTemperatureRange()).isEqualTo(range);
    }

    @Test
    @DisplayName("updateDefaults(invalid) -> NullPointerException; a null site leaves the defaults untouched (AAA)")
    void updateDefaultsRejectsInvalid() {
      // Arrange
      var config = dashboard();

      // Act & Assert
      assertThatThrownBy(() -> config.updateDefaults(null, temperatureRange()))
          .hasMessage("defaultSiteId must not be null");
      assertThat(config.getDefaultSiteId().siteId()).isEqualTo(SITE_ID);
      assertThatThrownBy(() -> config.updateDefaults(new SiteId(4L), null))
          .hasMessage("defaultTemperatureRange must not be null");
    }
  }

  @Nested
  class DashboardCardTest {

    @Test
    @DisplayName("new DashboardCard(type, order, visible) -> keeps values with no id (AAA)")
    void construction() {
      // Act
      var card = new DashboardCard(CardType.OPEN_ALERTS, 2, false);

      // Assert
      assertThat(card.getCardId()).isNull();
      assertThat(card.getCardType()).isEqualTo(CardType.OPEN_ALERTS);
      assertThat(card.getOrder()).isEqualTo(2);
      assertThat(card.isVisible()).isFalse();
    }

    @Test
    @DisplayName("new DashboardCard(invalid) -> rejects null type/order and order below 1 (AAA)")
    void invalidConstruction() {
      // Act & Assert
      assertThatThrownBy(() -> new DashboardCard(null, 1, true)).hasMessage("cardType must not be null");
      assertThatThrownBy(() -> new DashboardCard(CardType.OPEN_ALERTS, null, true))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("Card order must not be null");
      assertThatThrownBy(() -> new DashboardCard(CardType.OPEN_ALERTS, 0, true))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("Card order must be 1 or greater");
    }

    @Test
    @DisplayName("updateOrder(n) -> accepts positive values and rejects null or below 1 (AAA)")
    void updateOrder() {
      // Arrange
      var card = new DashboardCard(CardType.OPEN_ALERTS, 1, true);

      // Act
      card.updateOrder(3);

      // Assert
      assertThat(card.getOrder()).isEqualTo(3);
      assertThatThrownBy(() -> card.updateOrder(null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> card.updateOrder(0)).isInstanceOf(IllegalArgumentException.class);
      assertThat(card.getOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("setDashboardConfig(other) once attached -> IllegalStateException; same config is idempotent (AAA)")
    void cannotMoveBetweenDashboards() {
      // Arrange
      var first = dashboard();
      var second = dashboard();
      var card = first.getCards().get(0);

      // Act & Assert
      card.setDashboardConfig(first);
      assertThatThrownBy(() -> card.setDashboardConfig(second))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Card already belongs to another dashboard configuration");
      assertThatThrownBy(() -> card.setDashboardConfig(null)).hasMessage("dashboardConfig must not be null");
    }

    @Test
    @DisplayName("persisted cards fixture -> ids 101..104 in card-type order (AAA)")
    void persistedCardsFixture() {
      // Act & Assert
      assertThat(persistedCards()).extracting(DashboardCard::getCardId).containsExactly(101L, 102L, 103L, 104L);
    }
  }
}
