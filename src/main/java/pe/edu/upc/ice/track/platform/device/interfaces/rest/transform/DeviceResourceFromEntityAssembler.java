package pe.edu.upc.ice.track.platform.device.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.device.application.results.DeviceDetails;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceDetailsPage;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceStatusResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.PagedDeviceResource;

import java.util.List;

/**
 * Converts a Device aggregate, described with the unit it is linked to, into its safe public REST
 * representation.
 *
 * <p>Every conversion starts from a {@link DeviceDetails}, so no response can leave without the name
 * and the site of the paired unit.</p>
 */
public final class DeviceResourceFromEntityAssembler {
  private DeviceResourceFromEntityAssembler() {
  }

  public static DeviceResource toResourceFromDetails(DeviceDetails details) {
    var device = details.device();
    return new DeviceResource(
        device.getDeviceId(),
        device.getDeviceUid(),
        device.getBoardModel(),
        device.getFirmwareVersion(),
        DeviceStatusResource.fromDomain(device.getStatus()),
        device.getPairedEquipmentId(),
        details.pairedEquipmentName(),
        details.pairedSiteId(),
        device.getLastSeenAt());
  }

  /**
   * Converts a list of described devices, keeping its order.
   *
   * @param details the devices, already ordered
   * @return the resources, in the same order
   */
  public static List<DeviceResource> toResourcesFromDetails(List<DeviceDetails> details) {
    return details.stream().map(DeviceResourceFromEntityAssembler::toResourceFromDetails).toList();
  }

  /**
   * Converts a {@link DeviceDetailsPage} into its outbound paged resource.
   *
   * @param page the page to convert
   * @return the {@link PagedDeviceResource} resource
   */
  public static PagedDeviceResource toPagedResourceFromPage(DeviceDetailsPage page) {
    return new PagedDeviceResource(
        toResourcesFromDetails(page.content()),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }
}
