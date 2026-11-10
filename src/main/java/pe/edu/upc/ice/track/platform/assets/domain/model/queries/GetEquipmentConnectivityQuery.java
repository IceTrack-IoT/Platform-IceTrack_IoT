package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;

import java.util.List;

/**
 * Query to evaluate the connectivity of units the caller already reached, all at one server instant.
 *
 * <p>It carries units, not identifiers, on purpose: they were already scoped to the caller by the
 * read that produced them, so this query cannot be used to learn anything about another owner's
 * units.</p>
 *
 * @param equipment the units to evaluate; {@code null} means none
 */
public record GetEquipmentConnectivityQuery(List<Equipment> equipment) {

  /** Normalizes a missing list to an empty, immutable one. */
  public GetEquipmentConnectivityQuery {
    equipment = equipment == null ? List.of() : List.copyOf(equipment);
  }
}
