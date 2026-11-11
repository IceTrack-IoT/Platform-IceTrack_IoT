package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeThresholdCommand;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ChangeThresholdResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.IfMatchHeaderAssembler;

/**
 * Assembler that converts a {@link ChangeThresholdResource} into a {@link ChangeThresholdCommand}.
 */
public class ChangeThresholdCommandFromResourceAssembler {

  /**
   * Converts the threshold change payload into its command representation.
   *
   * @param equipmentId identifier of the unit, taken from the request path
   * @param username username of the authenticated caller
   * @param resource the {@link ChangeThresholdResource} payload
   * @param ifMatch the raw {@code If-Match} header, or {@code null}
   * @return the {@link ChangeThresholdCommand} command
   */
  public static ChangeThresholdCommand toCommandFromResource(
      Long equipmentId, String username, ChangeThresholdResource resource, String ifMatch) {
    return new ChangeThresholdCommand(
        equipmentId, username, resource.minCelsius(), resource.maxCelsius(),
        IfMatchHeaderAssembler.toExpectedVersion(ifMatch));
  }
}
