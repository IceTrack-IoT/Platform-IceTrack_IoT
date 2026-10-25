package pe.edu.upc.ice.track.platform.reporting.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.ReportFileStorage;
import pe.edu.upc.ice.track.platform.reporting.application.queryservices.ReportQueryService;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportDownloadLinkQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportsByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportDownloadLink;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.UserId;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link ReportQueryService}.
 */
@Service
public class ReportQueryServiceImpl implements ReportQueryService {

  private final ReportRepository reportRepository;
  private final ReportFileStorage fileStorage;
  private final Duration linkTtl;


  public ReportQueryServiceImpl(
      ReportRepository reportRepository,
      ReportFileStorage fileStorage,
      @Value("${reporting.storage.signed-url-ttl-minutes:10}") long ttlMinutes) {
    this.reportRepository = reportRepository;
    this.fileStorage = fileStorage;
    this.linkTtl = Duration.ofMinutes(ttlMinutes);
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

  @Override
  public Optional<ReportDownloadLink> handle(GetReportDownloadLinkQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    return reportRepository.findById(query.reportId())
        .filter(report -> report.isOwnedBy(new UserId(query.userId())))
        .filter(Report::hasFile)
        .map(report -> new ReportDownloadLink(
            fileStorage.generateDownloadUrl(report.getFile().key(), linkTtl),
            linkTtl.toSeconds()));
  }
}
