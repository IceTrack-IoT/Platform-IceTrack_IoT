package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;

import java.util.Set;

/**
 * Query for the equipment directory of an owner: the identity of every unit it owns, with the site
 * each one is installed at.
 *
 * <p>The directory is what the other screens of the owner's application resolve a unit by, so it is
 * answered whole, never paged. It can be narrowed to one site and to a set of statuses.</p>
 *
 * @param username username of the authenticated caller, resolved by the query service to the owner
 *                 whose units are listed; required
 * @param siteId   restrict to a single site of the caller, may be {@code null}
 * @param statuses restrict to these lifecycle statuses; empty does not restrict
 */
public record GetEquipmentDirectoryQuery(String username, Long siteId, Set<StatusEquipment> statuses) {

  /**
   * Normalizes a missing status filter to "no restriction".
   */
  public GetEquipmentDirectoryQuery {
    statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
  }
}
