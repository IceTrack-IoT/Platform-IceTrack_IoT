package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.projections;

/**
 * Closed projection of a {@code ReviewPersistenceEntity} onto its three scores.
 *
 * <p>Lets Spring Data select the three score columns alone when only a rating is being computed,
 * with no comment, timestamps or identifiers loaded.</p>
 */
public interface ReviewScoresProjection {

  /** @return how well the technician kept the owner informed, 1 to 5 */
  Integer getCommunication();

  /** @return whether the intervention solved the problem, 1 to 5 */
  Integer getEfficacy();

  /** @return how well the work itself was carried out, 1 to 5 */
  Integer getPerformance();
}
