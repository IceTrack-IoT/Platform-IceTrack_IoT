package pe.edu.upc.ice.track.platform.device.application.results;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceConnectivity;

/**
 * A board together with what a client needs to show the unit it is linked to and whether it is
 * reporting.
 *
 * <p>The name and the site of the unit belong to Assets Management and are only read from it, so
 * they are carried next to the aggregate instead of inside it. Both are {@code null} for a board
 * linked to no unit, and when the unit it is linked to no longer exists.</p>
 *
 * <p>The connectivity is evaluated when the board is read, with the offline window of the policy that
 * applies to its unit.</p>
 *
 * @param device              the board
 * @param pairedEquipmentName name of the unit the board is linked to, or {@code null}
 * @param pairedSiteId        identifier of the site that unit is installed at, or {@code null}
 * @param connectivity        the connectivity evaluated for the board; required
 */
public record DeviceDetails(Device device, String pairedEquipmentName, Long pairedSiteId, DeviceConnectivity connectivity) {

  /**
   * Describes a board whose unit is unknown: unpaired, or linked to a unit that no longer exists.
   *
   * @param device       the board
   * @param connectivity the connectivity evaluated for the board
   * @return the board with no unit name and no site
   */
  public static DeviceDetails withoutEquipment(Device device, DeviceConnectivity connectivity) {
    return new DeviceDetails(device, null, null, connectivity);
  }
}
