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
   * <p>For a caller acting as an owner, a unit belonging to somebody else is reported as empty,
   * exactly like a unit that does not exist, and so is every unit when the caller holds no owner
   * profile. A caller acting as a technician reaches any unit.</p>
   *
   * @param query The {@link GetEquipmentByIdQuery} Query
   * @return the unit, or empty when it does not exist or is out of the caller's reach
   */
  Optional<Equipment> handle(GetEquipmentByIdQuery query);

  /**
   * List the units installed at a site the caller owns, or at any site for a technician.
   *
   * @param query The {@link GetEquipmentBySiteQuery} Query
   * @return the units of that site, empty when the site does not exist or, for an owner, is not
   *         the caller's
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