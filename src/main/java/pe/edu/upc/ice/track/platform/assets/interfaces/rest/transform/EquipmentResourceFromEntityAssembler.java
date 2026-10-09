package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentPage;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentTypeResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.PagedEquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.StatusEquipmentResource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Assembler to convert an {@link Equipment} aggregate to an {@link EquipmentResource}.
 */
public class EquipmentResourceFromEntityAssembler {

  /**
   * Converts an {@link Equipment} aggregate to an {@link EquipmentResource}.
   *
   * @param entity the {@link Equipment} aggregate to convert
   * @return the {@link EquipmentResource} resource
   */
  public static EquipmentResource toResourceFromEntity(Equipment entity) {
    var threshold = entity.getTemperatureThreshold();
    return new EquipmentResource(
        entity.getEquipmentId(),
        entity.getSiteId(),
        entity.getUid(),
        entity.getName(),
        EquipmentTypeResource.fromDomain(entity.getEquipmentType()),
        StatusEquipmentResource.fromDomain(entity.getStatus()),
        entity.isOnline(),
        threshold.minCelsius(),
        threshold.maxCelsius(),
        entity.getReminderIntervalDays(),
        toIsoString(entity.getLastReadingAt()),
        entity.getLastKnownTemperature());
  }

  /**
   * Converts an {@link EquipmentPage} into its outbound paged resource.
   *
   * @param page the page to convert
   * @return the {@link PagedEquipmentResource} resource
   */
  public static PagedEquipmentResource toPagedResourceFromPage(EquipmentPage page) {
    return new PagedEquipmentResource(
        page.content().stream().map(EquipmentResourceFromEntityAssembler::toResourceFromEntity).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }

  private static String toIsoString(LocalDateTime value) {
    return value == null ? null : value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }
}
