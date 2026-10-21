package pe.edu.upc.ice.track.platform.reporting.application.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;
import pe.edu.upc.ice.track.platform.reporting.interfaces.acl.ReportingContextFacade;

import java.util.Comparator;
import java.util.List;

/**
 * Default implementation of {@link ReportingContextFacade}.
 *
 * <p>Delegates directly to {@link ReportRepository}: both operations only need to read already
 * persisted reports, so there is no need to route through {@code ReportQueryService}.</p>
 */
@Service
public class ReportingContextFacadeImpl implements ReportingContextFacade {

  private final ReportRepository reportRepository;

  public ReportingContextFacadeImpl(ReportRepository reportRepository) {
    this.reportRepository = reportRepository;
  }

  @Override
  public int countReportsByUser(Long userId) {
    if (userId == null) {
      return 0;
    }
    return reportRepository.findByUserId(userId).size();
  }

  @Override
  public String fetchLatestReportStatusByUser(Long userId) {
    if (userId == null) {
      return null;
    }
    List<Report> reports = reportRepository.findByUserId(userId);
    return reports.stream()
        .max(Comparator.comparing(Report::getRequestedAt))
        .map(report -> report.getStatus().name())
        .orElse(null);
  }
}
