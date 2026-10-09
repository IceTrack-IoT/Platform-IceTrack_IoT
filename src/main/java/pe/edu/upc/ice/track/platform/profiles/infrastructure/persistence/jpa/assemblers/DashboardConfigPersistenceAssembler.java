package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.model.entities.DashboardCard;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TemperatureRange;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.TemperatureRangePersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.DashboardCardPersistenceEntity;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.DashboardConfigPersistenceEntity;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Static assembler between dashboard configuration domain and persistence representations.
 */
public final class DashboardConfigPersistenceAssembler {

  private DashboardConfigPersistenceAssembler() {
  }

  public static DashboardConfig toDomainFromPersistence(DashboardConfigPersistenceEntity entity) {
    if (entity == null) return null;
    var temperatureRange = entity.getDefaultTemperatureRange();
    return new DashboardConfig(
        entity.getId(),
        new UserId(entity.getUserId()),
        new SiteId(entity.getDefaultSiteId()),
        new TemperatureRange(
            temperatureRange.getMin(),
            temperatureRange.getMax(),
            temperatureRange.getUnit(),
            temperatureRange.getLabel()),
        entity.getCards().stream()
            .map(DashboardConfigPersistenceAssembler::toDomainFromPersistence)
            .toList());
  }

  /**
   * Copies the state of a dashboard configuration onto its persistence entity, which is either new
   * or the managed entity the configuration was loaded from.
   *
   * <p>The cards are reconciled by identity rather than replaced, and never removed: a card the
   * aggregate holds with an identity updates its managed row in place, so a new order or visibility
   * is written by dirty checking; a card the aggregate holds without an identity - one provisioned
   * with a new configuration, or for a type a stored configuration lacked - is attached, and
   * therefore inserted on save.</p>
   *
   * @param dashboardConfig the dashboard configuration
   * @param entity          the persistence entity to update
   * @throws IllegalStateException when the aggregate holds a persisted card that is not one of this entity's cards
   */
  public static void copyToPersistence(DashboardConfig dashboardConfig, DashboardConfigPersistenceEntity entity) {
    entity.setUserId(dashboardConfig.getUserId().userId());
    entity.setDefaultSiteId(dashboardConfig.getDefaultSiteId().siteId());
    var temperatureRange = dashboardConfig.getDefaultTemperatureRange();
    entity.setDefaultTemperatureRange(new TemperatureRangePersistenceEmbeddable(
        temperatureRange.min(),
        temperatureRange.max(),
        temperatureRange.unit(),
        temperatureRange.label()));

    Map<Long, DashboardCardPersistenceEntity> cardEntitiesById = entity.getCards().stream()
        .collect(Collectors.toMap(DashboardCardPersistenceEntity::getId, Function.identity()));

    for (var card : dashboardConfig.getCards()) {
      if (card.getCardId() == null) {
        var cardEntity = new DashboardCardPersistenceEntity();
        copyToPersistence(card, cardEntity);
        entity.addCard(cardEntity);
        continue;
      }
      var cardEntity = cardEntitiesById.get(card.getCardId());
      if (cardEntity == null) {
        throw new IllegalStateException("Card %s does not belong to dashboard configuration %s"
            .formatted(card.getCardId(), dashboardConfig.getDashboardConfigId()));
      }
      copyToPersistence(card, cardEntity);
    }
  }

  private static DashboardCard toDomainFromPersistence(DashboardCardPersistenceEntity entity) {
    return new DashboardCard(entity.getId(), entity.getCardType(), entity.getCardOrder(), entity.isVisible());
  }

  private static void copyToPersistence(DashboardCard card, DashboardCardPersistenceEntity entity) {
    entity.setCardType(card.getCardType());
    entity.setCardOrder(card.getOrder());
    entity.setVisible(card.isVisible());
  }
}
