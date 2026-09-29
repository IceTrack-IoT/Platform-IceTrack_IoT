package pe.edu.upc.ice.track.platform.profiles.application.queryservices;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllOwnersQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByUserIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Owner Query Service
 */
public interface OwnerQueryService {

  /**
   * Handle Get Owner By Id Query.
   *
   * @param query the query
   * @return the owner profile, or empty when not found
   */
  Optional<OwnerProfile> handle(GetOwnerByIdQuery query);

  /**
   * Handle Get Owner By User Id Query.
   *
   * @param query the query
   * @return the owner profile, or empty when not found
   */
  Optional<OwnerProfile> handle(GetOwnerByUserIdQuery query);

  /**
   * Handle Get All Owners Query.
   *
   * @param query the query
   * @return every owner profile
   */
  List<OwnerProfile> handle(GetAllOwnersQuery query);
}
