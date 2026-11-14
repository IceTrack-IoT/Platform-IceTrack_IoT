package pe.edu.upc.ice.track.platform.reporting.domain.repositories;

import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportListFilter;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportPage;

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
   *  Retrieves every report requested by a user that matches a filter, most recent first.
   * @param userId the identifier of the requesting user
   * @param filter name, type, status and request-date restrictions; never widens the user scope
   * @return  the matching reports, empty when there are none
   */
  List<Report> findByUserId(Long userId, ReportListFilter filter);

  /**
   *  Retrieves one page of the reports requested by a user that match a filter, most recent first.
   * @param userId the identifier of the requesting user
   * @param filter name, type, status and request-date restrictions; never widens the user scope
   * @param page   zero-based page index; not negative
   * @param size   maximum number of reports on the page; strictly positive
   * @return  the reports of that page and the total count
   */
  ReportPage findPageByUserId(Long userId, ReportListFilter filter, int page, int size);

  /**
   *  Deletes a report. Deleting an identifier that names no report changes nothing.
   * @param reportId the identifier of the report to delete
   */
  void deleteById(Long reportId);
}
