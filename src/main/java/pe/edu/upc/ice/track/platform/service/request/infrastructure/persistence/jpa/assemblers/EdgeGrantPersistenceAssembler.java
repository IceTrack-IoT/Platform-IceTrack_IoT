package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.EdgeGrant;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities.EdgeGrantPersistenceEntity;

/**
 * Static assembler between the {@link EdgeGrant} aggregate and its {@link EdgeGrantPersistenceEntity}.
 */
public final class EdgeGrantPersistenceAssembler {

  private EdgeGrantPersistenceAssembler() {
  }

  public static EdgeGrant toDomainFromPersistence(EdgeGrantPersistenceEntity entity) {
    return new EdgeGrant(
        entity.getId(),
        entity.getServiceRequestId(),
        entity.getTechnicianProfileId(),
        entity.getEquipmentId(),
        entity.getTokenDigest(),
        entity.getIssuedAt(),
        entity.getExpiresAt(),
        entity.getRevokedAt(),
        entity.getRevocationReason());
  }

  public static EdgeGrantPersistenceEntity toPersistenceFromDomain(EdgeGrant grant) {
    var entity = new EdgeGrantPersistenceEntity();
    entity.setId(grant.getId());
    entity.setServiceRequestId(grant.getServiceRequestId());
    entity.setTechnicianProfileId(grant.getTechnicianProfileId());
    entity.setEquipmentId(grant.getEquipmentId());
    entity.setTokenDigest(grant.getTokenDigest());
    entity.setIssuedAt(grant.getIssuedAt());
    entity.setExpiresAt(grant.getExpiresAt());
    entity.setRevokedAt(grant.getRevokedAt());
    entity.setRevocationReason(grant.getRevocationReason());
    return entity;
  }
}
