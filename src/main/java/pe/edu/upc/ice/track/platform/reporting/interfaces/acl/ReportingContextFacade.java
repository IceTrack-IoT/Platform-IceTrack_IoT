package pe.edu.upc.ice.track.platform.reporting.interfaces.acl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportListFilter;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;

/**
 * Anti-Corruption Layer facade exposing {@code reporting} to other bounded contexts (e.g.
 * {@code profiles}'s {@code DashboardConfig}, which may want a quick KPI number without
 * requesting and polling a full, persisted {@link Report}).
 *
 * <p>As with every other ACL facade in this codebase, every parameter and return type here is a
 * primitive, a boxed primitive, or a {@code String} -- never a {@code reporting} aggregate, value
 * object, command or repository.</p>
 *
 * <p>Reads go straight to {@link ReportRepository}: both operations only need already persisted
 * reports, so there is no need to route through {@code ReportQueryService}.</p>
 */
@Service
public class ReportingContextFacade {

  private final ReportRepository reportRepository;

  public ReportingContextFacade(ReportRepository reportRepository) {
    this.reportRepository = reportRepository;
  }

  /**
   * Returns the count of reports a user has requested in total, regardless of status. Intended
   * for a lightweight dashboard tile rather than a full report listing.
   *
   * @param userId identifier of the user
   * @return how many reports the user has requested
   */
  @Transactional(readOnly = true)
  public int countReportsByUser(Long userId) {
    if (userId == null) {
      return 0;
    }
    // A one-row page carries the total, so the reports themselves are never loaded.
    return Math.toIntExact(reportRepository.findPageByUserId(userId, ReportListFilter.NONE, 0, 1).totalElements());
  }

  /**
   * Returns the status of the most recently requested report for a user, or {@code null} when
   * the user has never requested one.
   *
   * @param userId identifier of the user
   * @return the latest report's status name (e.g. {@code "COMPLETED"}), or {@code null}
   */
  @Transactional(readOnly = true)
  public String fetchLatestReportStatusByUser(Long userId) {
    if (userId == null) {
      return null;
    }
    // The listing is ordered most recently requested first, so the first row of the first page is
    // the latest report.
    return reportRepository.findPageByUserId(userId, ReportListFilter.NONE, 0, 1).content().stream()
        .findFirst()
        .map(report -> report.getStatus().name())
        .orElse(null);
  }
}
