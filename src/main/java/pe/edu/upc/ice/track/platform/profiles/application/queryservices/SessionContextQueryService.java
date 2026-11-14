package pe.edu.upc.ice.track.platform.profiles.application.queryservices;

import pe.edu.upc.ice.track.platform.profiles.application.results.SessionContext;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetSessionContextQuery;

import java.util.Optional;

/**
 * Application service answering the session bootstrap read.
 */
public interface SessionContextQueryService {

  /**
   * Reads the session context of the authenticated caller.
   *
   * @param query the {@link GetSessionContextQuery} to handle
   * @return the context, or empty when no account holds the caller's username anymore
   */
  Optional<SessionContext> handle(GetSessionContextQuery query);
}
