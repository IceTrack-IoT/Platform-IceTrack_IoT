package pe.edu.upc.ice.track.platform.reporting.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalProfilesServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.ReportFileStorage;
import pe.edu.upc.ice.track.platform.reporting.application.queryservices.ReportQueryService;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportDownloadLinkQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportPageByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportsByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportDownloadLink;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportPage;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
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
 *
 * <p>The queries returning reports run in a read-only transaction: {@code result_payload} is stored as a
 * large object, which the PostgreSQL driver only reads inside a transaction, never in auto-commit
 * mode.</p>
 */
@Service
public class ReportQueryServiceImpl implements ReportQueryService {

  /** How long a download link stays valid. */
  static final Duration SIGNED_URL_TTL = Duration.ofMinutes(15);

  private static final String REPORT_RESOURCE = "Report";
  private static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";

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
  @Transactional(readOnly = true)
  public Optional<Report> handle(GetReportByIdQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    return findOwnedReport(query.reportId(), query.username());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Report> handle(GetReportsByUserQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    return externalProfilesService.fetchUserIdByUsername(query.username())
        .map(userId -> reportRepository.findByUserId(userId, query.filter()))
        .orElse(List.of());
  }

  @Override
  @Transactional(readOnly = true)
  public ReportPage handle(GetReportPageByUserQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    return externalProfilesService.fetchUserIdByUsername(query.username())
        .map(userId -> reportRepository.findPageByUserId(userId, query.filter(), query.page(), query.size()))
        .orElseGet(() -> ReportPage.empty(query.page(), query.size()));
  }

  /**
   * Signs a download link for one of the caller's completed reports.
   *
   * <p>A report of anybody else answers {@code REPORT_NOT_FOUND}, exactly like a missing one. A
   * report that is not {@code COMPLETED} has no file yet, or never will, which is a state conflict
   * rather than an absence: {@code INVALID_STATUS_TRANSITION}, naming its status.</p>
   */
  @Override
  @Transactional(readOnly = true)
  public Result<ReportDownloadLink, ApplicationError> handle(GetReportDownloadLinkQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    var report = findOwnedReport(query.reportId(), query.username());
    if (report.isEmpty()) {
      return Result.failure(ApplicationError.notFound(REPORT_RESOURCE, String.valueOf(query.reportId())));
    }
    var found = report.get();
    if (!found.hasFile()) {
      return Result.failure(new ApplicationError(
          INVALID_STATUS_TRANSITION,
          "Report has no file to download",
          found.getStatus() == ReportStatus.COMPLETED
              ? "Report %d is COMPLETED but has no stored file".formatted(found.getId())
              : "Report %d is %s; a download link is only issued for a COMPLETED report"
                  .formatted(found.getId(), found.getStatus())));
    }
    var fileName = found.suggestedFileName();
    var signed = fileStorage.generateDownloadUrl(found.getFile().key(), linkTtl, fileName);
    return Result.success(new ReportDownloadLink(
        signed.url(),
        signed.expiresAt(),
        fileName,
        found.getFile().contentType(),
        found.getFile().sizeBytes()));
  }

  private Optional<Report> findOwnedReport(Long reportId, String username) {
    if (reportId == null) {
      return Optional.empty();
    }
    return externalProfilesService.fetchUserIdByUsername(username)
        .flatMap(userId -> reportRepository.findById(reportId)
            .filter(report -> report.isOwnedBy(new UserId(userId))));
  }
}
