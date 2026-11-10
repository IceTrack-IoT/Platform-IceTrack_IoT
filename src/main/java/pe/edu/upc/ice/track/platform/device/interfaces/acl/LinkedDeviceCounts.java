package pe.edu.upc.ice.track.platform.device.interfaces.acl;

/**
 * Published count of the boards linked to a set of units, returned by
 * {@link DeviceContextFacade#countLinkedDevices}.
 *
 * <p>Part of the published language of the {@code device} context: plain values only.</p>
 *
 * @param paired  boards paired with one of the units and able to report
 * @param revoked boards whose credential was withdrawn but that are still linked to one of the units
 */
public record LinkedDeviceCounts(long paired, long revoked) {
}
