package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentConnectivity;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentPage;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ConnectivityStateResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentConnectivityResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentTypeResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.PagedEquipmentResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.StatusEquipmentResource;

import java.util.Map;


/**
 * Assembler to convert an {@link Equipment} aggregate to an {@link EquipmentResource}.
 *
 * <p>The connectivity comes from an evaluation made beforehand for every unit being converted, at
 * one server instant, so converting a page reads nothing.</p>
 */
public class EquipmentResourceFromEntityAssembler {

  /**
   * Converts an {@link Equipment} aggregate to an {@link EquipmentResource}.
   *
   * @param entity       the {@link Equipment} aggregate to convert
   * @param connectivity the connectivity evaluated for it, or {@code null} when none was
   * @return the {@link EquipmentResource} resource
   */
  public static EquipmentResource toResourceFromEntity(Equipment entity, EquipmentConnectivity connectivity) {
    var threshold = entity.getTemperatureThreshold();
    return new EquipmentResource(
        entity.getEquipmentId(),
        entity.getSiteId(),
        entity.getSiteName(),
        entity.getUid(),
        entity.getName(),
        EquipmentTypeResource.fromDomain(entity.getEquipmentType()),
        StatusEquipmentResource.fromDomain(entity.getStatus()),
        entity.isOnline(),
        threshold.minCelsius(),
        threshold.maxCelsius(),
        entity.getReminderIntervalDays(),
        entity.getLastReadingAt(),
        entity.getLastKnownTemperature(),
        entity.getConfigVersion(),
        toResourceFromConnectivity(connectivity),
        entity.getVersion() == null ? 0L : entity.getVersion());
  }

  /**
   * Converts an {@link EquipmentPage} into its outbound paged resource.
   *
   * @param page         the page to convert
   * @param connectivity the connectivity evaluated for the units of the page, by unit identifier
   * @return the {@link PagedEquipmentResource} resource
   */
  public static PagedEquipmentResource toPagedResourceFromPage(
      EquipmentPage page, Map<Long, EquipmentConnectivity> connectivity) {
    return new PagedEquipmentResource(
        page.content().stream()
            .map(unit -> toResourceFromEntity(unit, connectivity.get(unit.getEquipmentId())))
            .toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }

  /**
   * Converts an evaluated connectivity into its resource.
   *
   * @param connectivity the connectivity, may be {@code null}
   * @return the resource, or {@code null} when there is none
   */
  public static EquipmentConnectivityResource toResourceFromConnectivity(EquipmentConnectivity connectivity) {
    if (connectivity == null) {
      return null;
    }
    return new EquipmentConnectivityResource(
        ConnectivityStateResource.fromDomain(connectivity.state()),
        connectivity.lastReceivedAt(),
        connectivity.silentAfter().toSeconds(),
        connectivity.silentSince(),
        connectivity.underMaintenance(),
        connectivity.evaluatedAt());
  }
}
