package pe.edu.upc.ice.track.platform.reporting.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.queryservices.ReportQueryService;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportsByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link ReportQueryService}.
 */
@Service
public class ReportQueryServiceImpl implements ReportQueryService {

  private final ReportRepository reportRepository;

  public ReportQueryServiceImpl(ReportRepository reportRepository) {
    this.reportRepository = reportRepository;
  }

  @Override
  public Optional<Report> handle(GetReportByIdQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    return reportRepository.findById(query.reportId());
  }

  @Override
  public List<Report> handle(GetReportsByUserQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    return reportRepository.findByUserId(query.userId());
  }
}
