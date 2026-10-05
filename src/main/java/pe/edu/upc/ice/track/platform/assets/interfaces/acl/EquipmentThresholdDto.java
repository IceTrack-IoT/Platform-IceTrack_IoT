package pe.edu.upc.ice.track.platform.assets.interfaces.acl;

/**
 * Acceptable temperature band of a unit, in the published language of the {@code assets} context.
 *
 * <p>A flat record rather than the domain {@code TemperatureThreshold} on purpose: this DTO is
 * what crosses the Anti-Corruption Layer, so a caller can hold a threshold without holding an
 * assets type whose invariants, or whose future changes, become their problem.</p>
 *
 * @param minCelsius the lowest acceptable temperature in Celsius
 * @param maxCelsius the highest acceptable temperature in Celsius
 */
public record EquipmentThresholdDto(Double minCelsius, Double maxCelsius) {
}