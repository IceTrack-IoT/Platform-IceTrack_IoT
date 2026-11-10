package pe.edu.upc.ice.track.platform.service.request.domain.model.commands;

import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServicePriority;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType;

/**
 * Command to create a service request for one of the caller's units.
 *
 * <p>Carries no owner and no site: the owner is resolved from the caller, and the site is the one
 * the unit is installed at, both checked through the outbound ACL so that a request can only ever
 * be opened against a unit its owner actually has.</p>
 *
 * @param username      username of the authenticated caller, resolved by the command service to the
 *                      owner the unit must belong to; required
 * @param equipmentId   identifier of the unit to service; required
 * @param type          the kind of work; required
 * @param priority      the urgency; required
 * @param description   what the owner needs; required
 * @param sourceAlertId the alert the request is opened for, which must be the caller's and raised on
 *                      the same unit; optional
 */
public record CreateServiceRequestCommand(
    String username,
    Long equipmentId,
    ServiceType type,
    ServicePriority priority,
    String description,
    Long sourceAlertId) {
}
