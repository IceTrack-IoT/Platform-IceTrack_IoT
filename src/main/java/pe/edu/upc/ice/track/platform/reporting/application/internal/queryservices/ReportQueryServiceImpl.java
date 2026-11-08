package pe.edu.upc.ice.track.platform.reporting.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalProfilesServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.ReportFileStorage;
import pe.edu.upc.ice.track.platform.reporting.application.queryservices.ReportQueryService;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportDownloadLinkQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportsByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportDownloadLink;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link ReportQueryService}.
 *
 * <p>Every query is scoped to the authenticated caller: the username is resolved to its
 * platform account, and a report requested by anybody else is answered as absent.</p>
 */
@Service
public class ReportQueryServiceImpl implements ReportQueryService {

  /** How long a download link stays valid. */
  static final Duration SIGNED_URL_TTL = Duration.ofMinutes(15);

  private final ReportRepository reportRepository;
  private final ReportFileStorage fileStorage;
  private final Duration linkTtl;
  private final ExternalProfilesServiceForReporting externalProfilesService;

  public ReportQueryServiceImpl(
      ReportRepository reportRepository,
      ReportFileStorage fileStorage,
      ExternalProfilesServiceForReporting externalProfilesService) {
    this.reportRepository = reportRepository;
    this.fileStorage = fileStorage;
    this.linkTtl = SIGNED_URL_TTL;
    this.externalProfilesService = externalProfilesService;
  }

  @Override
  public Optional<Report> handle(GetReportByIdQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    return findOwnedReport(query.reportId(), query.username());
  }

  @Override
  public List<Report> handle(GetReportsByUserQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    return externalProfilesService.fetchUserIdByUsername(query.username())
        .map(reportRepository::findByUserId)
        .orElse(List.of());
  }

  @Override
  public Optional<ReportDownloadLink> handle(GetReportDownloadLinkQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    return findOwnedReport(query.reportId(), query.username())
        .filter(Report::hasFile)
        .map(report -> new ReportDownloadLink(
            fileStorage.generateDownloadUrl(report.getFile().key(), linkTtl),
            linkTtl.toSeconds()));
  }

  private Optional<Report> findOwnedReport(Long reportId, String username) {
    return externalProfilesService.fetchUserIdByUsername(username)
        .flatMap(userId -> reportRepository.findById(reportId)
            .filter(report -> report.isOwnedBy(new UserId(userId))));
  }
}
