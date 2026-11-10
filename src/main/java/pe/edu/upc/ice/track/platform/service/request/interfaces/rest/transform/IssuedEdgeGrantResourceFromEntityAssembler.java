package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.IssuedEdgeGrant;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.IssuedEdgeGrantResource;

/**
 * Assembler converting an {@link IssuedEdgeGrant} into an {@link IssuedEdgeGrantResource}.
 */
public final class IssuedEdgeGrantResourceFromEntityAssembler {

  private IssuedEdgeGrantResourceFromEntityAssembler() {
  }

  public static IssuedEdgeGrantResource toResourceFromEntity(IssuedEdgeGrant issued) {
    var grant = issued.grant();
    return new IssuedEdgeGrantResource(
        issued.token(),
        grant.getServiceRequestId(),
        grant.getTechnicianProfileId(),
        grant.getEquipmentId(),
        grant.getIssuedAt().toString(),
        grant.getExpiresAt().toString());
  }
}
