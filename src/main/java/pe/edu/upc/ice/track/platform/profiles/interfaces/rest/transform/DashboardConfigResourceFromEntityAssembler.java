package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.DashboardConfig;
import pe.edu.upc.ice.track.platform.profiles.domain.model.entities.DashboardCard;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TemperatureRange;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.DashboardCardResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.DashboardConfigResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.TemperatureRangeResource;

/**
 * Assembler to convert a {@link DashboardConfig} aggregate, its cards and its value objects to
 * their REST resources.
 */
public class DashboardConfigResourceFromEntityAssembler {
  /**
   * Converts a DashboardConfig aggregate to a DashboardConfigResource.
   *
   * @param entity The {@link DashboardConfig} aggregate to convert.
   * @return The {@link DashboardConfigResource} resource, with its cards sorted by their order.
   */
  public static DashboardConfigResource toResourceFromEntity(DashboardConfig entity) {
    return new DashboardConfigResource(
        entity.getDashboardConfigId(),
        entity.getUserId().userId(),
        entity.getDefaultSiteId().siteId(),
        toResourceFromValueObject(entity.getDefaultTemperatureRange()),
        entity.getCards().stream()
            .map(DashboardConfigResourceFromEntityAssembler::toCardResourceFromEntity)
            .toList());
  }

  /**
   * Converts a DashboardCard entity to a DashboardCardResource.
   *
   * @param entity The {@link DashboardCard} entity to convert.
   * @return The {@link DashboardCardResource} resource.
   */
  public static DashboardCardResource toCardResourceFromEntity(DashboardCard entity) {
    return new DashboardCardResource(
        entity.getCardId(),
        entity.getCardType().name(),
        entity.getOrder(),
        entity.isVisible());
  }

  /**
   * Converts a TemperatureRange value object to a TemperatureRangeResource.
   *
   * @param valueObject The {@link TemperatureRange} value object to convert.
   * @return The {@link TemperatureRangeResource} resource.
   */
  public static TemperatureRangeResource toResourceFromValueObject(TemperatureRange valueObject) {
    return new TemperatureRangeResource(valueObject.value(), valueObject.label());
  }
}
