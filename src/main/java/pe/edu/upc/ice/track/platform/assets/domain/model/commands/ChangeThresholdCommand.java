package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to replace the acceptable temperature band of a unit.
 *
 * @param equipmentId identifier of the unit whose threshold changes; required
 * @param username    username of the authenticated caller, resolved by the command service to the
 *                    owner the unit must belong to; required
 * @param minCelsius  the new lowest acceptable temperature in Celsius; required and strictly below
 *                    {@code maxCelsius}
 * @param maxCelsius  the new highest acceptable temperature in Celsius; required and strictly above
 *                    {@code minCelsius}
 */
public record ChangeThresholdCommand(
    Long equipmentId,
    String username,
    Double minCelsius,
    Double maxCelsius) {
}