package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.profiles.application.results.SessionContext;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.SessionContextResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.SessionCountsResource;
import pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources.SessionUserResource;

/**
 * Assembler to convert a {@link SessionContext} into a {@link SessionContextResource}.
 */
public final class SessionContextResourceFromResultAssembler {

  private SessionContextResourceFromResultAssembler() {
  }

  /**
   * Converts a session context into its resource. The owner profile is shown in full, since it is the
   * caller's own.
   *
   * @param context the session context
   * @return the resource
   */
  public static SessionContextResource toResourceFromResult(SessionContext context) {
    return new SessionContextResource(
        new SessionUserResource(
            context.userId(), context.username(), context.email(), context.role(), context.provider()),
        context.ownerProfile() == null
            ? null
            : OwnerProfileResourceFromEntityAssembler.toResourceFromEntity(context.ownerProfile()),
        context.dashboardConfig() == null
            ? null
            : DashboardConfigResourceFromEntityAssembler.toResourceFromEntity(context.dashboardConfig()),
        new SessionCountsResource(context.siteCount(), context.equipmentCount()),
        context.serverTime());
  }
}
