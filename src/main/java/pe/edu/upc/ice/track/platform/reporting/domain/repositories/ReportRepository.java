package pe.edu.upc.ice.track.platform.reporting.domain.repositories;

import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository port for {@link Report}.
 *
 * <p>Implemented by {@code ReportRepositoryImpl} in the infrastructure layer, which delegates to
 * the Spring Data JPA repository and the persistence assembler.</p>
 */
public interface ReportRepository {

  /**
   *  Persists the given report in the database.
   * @param report the report to be persisted
   * @return  the persisted report with any generated values (e.g., ID)
   */
  Report save(Report report);

  /**
   *  Retrieves a report by its identifier.
   * @param reportId the identifier of the report to be retrieved
   * @return  an {@code Optional} containing the report if found, or empty if not found
   */
  Optional<Report> findById(Long reportId);

  /**
   *  Retrieves every report requested by a user, most recent first.
   * @param userId the identifier of the requesting user
   * @return  the user's reports, empty when they have none
   */
  List<Report> findByUserId(Long userId);
}
