package pe.edu.upc.ice.track.platform.profiles.application.queryservices;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllProfilesQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetProfileByEmailQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetProfileByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Application service contract for Profiles bounded-context read queries.
 */
public interface ProfileQueryService {
  /**
   * Handle Get Profile By ID Query
   *
   * @param query The {@link GetProfileByIdQuery} Query
   * @return A {@link Profile} instance if the query is valid, otherwise empty
   */
  Optional<Profile> handle(GetProfileByIdQuery query);

  /**
   * Handle Get Profile By Email Query
   *
   * @param query The {@link GetProfileByEmailQuery} Query
   * @return A {@link Profile} instance if the query is valid, otherwise empty
   */
  Optional<Profile> handle(GetProfileByEmailQuery query);

  /**
   * Handle Get Profile By User Id Query
   *
   * @param query The {@link GetProfileByUserIdQuery} Query
   * @return A {@link Profile} instance when the account already has a profile, otherwise empty
   */
  Optional<Profile> handle(GetProfileByUserIdQuery query);

  /**
   * Handle Get All Profiles Query
   *
   * @param query The {@link GetAllProfilesQuery} Query
   * @return A list of {@link Profile} instances
   */
  List<Profile> handle(GetAllProfilesQuery query);
}
