package pe.edu.upc.ice.track.platform.assets.application.queryservices;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByIdQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentByOwnerQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetEquipmentBySiteQuery;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentPage;

import java.util.List;
import java.util.Optional;

/**
 * Application service handling read operations on {@link Equipment}.
 *
 * <p>The listing returned by {@link #handle(GetEquipmentByOwnerQuery)} is the dashboard's hottest
 * read path, so it resolves its filters in the database rather than in memory.</p>
 */
public interface EquipmentQueryService {

  /**
   * Fetch a single equipment unit.
   *
   * <p>When the query carries an {@code ownerId}, a unit belonging to somebody else is reported as
   * empty, exactly like a unit that does not exist. When it carries none, the unit is returned
   * whatever its owner: that is the shape the ACL facade needs to answer other bounded
   * contexts.</p>
   *
   * @param query The {@link GetEquipmentByIdQuery} Query
   * @return the unit, or empty when it does not exist or is out of the caller's reach
   */
  Optional<Equipment> handle(GetEquipmentByIdQuery query);

  /**
   * List the units installed at a site the caller owns.
   *
   * @param query The {@link GetEquipmentBySiteQuery} Query
   * @return the units of that site, empty when the site does not exist or is not the caller's
   */
  List<Equipment> handle(GetEquipmentBySiteQuery query);

  /**
   * List a page of the caller's units, narrowed by the optional filters of the query.
   *
   * @param query The {@link GetEquipmentByOwnerQuery} Query
   * @return one {@link EquipmentPage} of matching units, paged or whole according to the query
   */
  EquipmentPage handle(GetEquipmentByOwnerQuery query);
}