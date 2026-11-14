package pe.edu.upc.ice.track.platform.reporting.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.reporting.application.commandservices.ReportCommandService;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalAssetsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalProfilesServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalServiceRequestsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.DeleteReportCommand;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.RequestReportCommand;
import pe.edu.upc.ice.track.platform.reporting.domain.model.events.ReportDeletedEvent;
import pe.edu.upc.ice.track.platform.reporting.domain.model.events.ReportRequestedEvent;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.DateRange;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.TechnicianId;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.SiteId;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.time.Instant;
import java.util.Objects;

/**
 * Default implementation of {@link ReportCommandService}.
 *
 * <p>Requesting a report only validates the request and saves it in {@code PENDING} status; it
 * answers at once, with the report identifier. The computation, rendering and upload happen later in
 * {@code ReportGenerationWorker}, which the {@link ReportRequestedEvent} starts after this transaction
 * commits. A failure in any of those steps never reaches the requester: the report ends {@code FAILED}
 * with a reason instead.</p>
 *
 * <p>The requester is always the authenticated caller: the command names it by username, resolved to
 * its platform account and owner profile through the {@link ExternalProfilesServiceForReporting} ACL.
 * A site or unit filter that does not belong to that owner is answered 404 before anything is saved.</p>
 *
 * <p>A {@code SERVICE_INTERVENTION} report documents one completed service request of the caller,
 * read through the Service Requests ACL; it takes the request's own period when none is given. A
 * finished report ({@code COMPLETED} or {@code FAILED}) can be deleted, and its stored file goes with
 * it once the deletion commits.</p>
 */
@Service
@Slf4j
public class ReportCommandServiceImpl implements ReportCommandService {

  private final ReportRepository reportRepository;
  private final ExternalProfilesServiceForReporting externalProfilesService;
  private final ExternalAssetsServiceForReporting externalAssetsService;
  private final ExternalServiceRequestsServiceForReporting externalServiceRequestsService;
  private final ApplicationEventPublisher eventPublisher;

  public ReportCommandServiceImpl(
      ReportRepository reportRepository,
      ExternalProfilesServiceForReporting externalProfilesService,
      ExternalAssetsServiceForReporting externalAssetsService,
      ExternalServiceRequestsServiceForReporting externalServiceRequestsService,
      ApplicationEventPublisher eventPublisher) {
    this.reportRepository = reportRepository;
    this.externalProfilesService = externalProfilesService;
    this.externalAssetsService = externalAssetsService;
    this.externalServiceRequestsService = externalServiceRequestsService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public Result<Report, ApplicationError> handle(RequestReportCommand command) {
    Objects.requireNonNull(command, "command must not be null");

    if (command.title() == null || command.title().isBlank()) {
      return Result.failure(ApplicationError.validationError("name", "The report name is required"));
    }
    if (command.type() == null || command.format() == null) {
      return Result.failure(ApplicationError.validationError("report",
          "The report type and format must be one of the values listed in the API documentation"));
    }

    var userId = externalProfilesService.fetchUserIdByUsername(command.username());
    if (userId.isEmpty()) {
      return Result.failure(ApplicationError.accessDenied("The caller has no platform account"));
    }
    var ownerId = externalProfilesService.fetchOwnerIdByUserId(userId.get());
    if (ownerId.isEmpty()) {
      return Result.failure(ApplicationError.accessDenied("Reports are available to owners only"));
    }
    if (command.siteId() != null && !externalAssetsService.isSiteOwnedBy(command.siteId(), ownerId.get())) {
      return Result.failure(ApplicationError.notFound("Site", String.valueOf(command.siteId())));
    }
    if (command.equipmentId() != null
        && !externalAssetsService.isEquipmentOwnedBy(command.equipmentId(), ownerId.get())) {
      return Result.failure(ApplicationError.notFound("Equipment", String.valueOf(command.equipmentId())));
    }

    DateRange period;
    switch (periodOf(command, ownerId.get())) {
      case Result.Success<DateRange, ApplicationError> success -> period = success.value();
      case Result.Failure<DateRange, ApplicationError> failure -> {
        return Result.failure(failure.error());
      }
    }

    try {
      var filters = new ReportFilters(
          command.siteId() != null ? new SiteId(command.siteId()) : null,
          command.equipmentId() != null ? new EquipmentId(command.equipmentId()) : null,
          command.technicianId() != null ? new TechnicianId(command.technicianId()) : null,
          period,
          command.serviceRequestId());
      var saved = reportRepository.save(new Report(
          command.title().trim(), command.type(), command.format(), new UserId(userId.get()), filters));
      eventPublisher.publishEvent(new ReportRequestedEvent(saved.getId()));
      log.info("Report {} requested as {} ({})", saved.getId(), command.type(), command.format());
      return Result.success(saved);
    } catch (IllegalArgumentException | NullPointerException ex) {
      return Result.failure(ApplicationError.validationError("report", ex.getMessage()));
    }
  }

  /**
   * Deletes one of the caller's finished reports. The stored file is removed by
   * {@code ReportFileCleanupEventHandler} once the deletion commits.
   */
  @Override
  @Transactional
  public Result<Long, ApplicationError> handle(DeleteReportCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    var userId = externalProfilesService.fetchUserIdByUsername(command.username());
    var report = userId.flatMap(id -> reportRepository.findById(command.reportId())
        .filter(candidate -> candidate.isOwnedBy(new UserId(id))));
    if (report.isEmpty()) {
      return Result.failure(ApplicationError.notFound("Report", String.valueOf(command.reportId())));
    }
    if (!report.get().canBeDeleted()) {
      return Result.failure(new ApplicationError(
          "INVALID_STATUS_TRANSITION",
          "The report is still being generated",
          "A report can be deleted once it is COMPLETED or FAILED; it is " + report.get().getStatus()));
    }
    var fileKey = report.get().getFile() != null ? report.get().getFile().key() : null;
    reportRepository.deleteById(command.reportId());
    eventPublisher.publishEvent(new ReportDeletedEvent(command.reportId(), fileKey));
    log.info("Report {} deleted", command.reportId());
    return Result.success(command.reportId());
  }

  /**
   * Decides the period of the report and checks the filters that depend on its type.
   *
   * <p>A service document must name a service request of the caller that is {@code COMPLETED}; its
   * period is the one given, or the request's own when none is. Every other type must name no service
   * request, and must give its period.</p>
   *
   * @param command the request
   * @param ownerId the owner profile of the caller
   * @return the period; or a 400 failure for a missing or refused filter, a 404 failure for a request
   *         that is not the caller's, or a 409 failure for a request that is not completed
   */
  private Result<DateRange, ApplicationError> periodOf(RequestReportCommand command, Long ownerId) {
    if (!command.type().isScopedByServiceRequest()) {
      if (command.serviceRequestId() != null) {
        return Result.failure(ApplicationError.validationError("filters.service_request_id",
            "service_request_id is only accepted for SERVICE_INTERVENTION reports"));
      }
      if (command.from() == null || command.to() == null) {
        return Result.failure(ApplicationError.validationError("filters",
            "from and to are required for %s reports".formatted(command.type())));
      }
      return validRange(command.from(), command.to());
    }
    if (command.serviceRequestId() == null) {
      return Result.failure(ApplicationError.validationError("filters.service_request_id",
          "service_request_id is required for SERVICE_INTERVENTION reports"));
    }
    var request = externalServiceRequestsService.fetchServiceIntervention(command.serviceRequestId())
        .filter(candidate -> ownerId.equals(candidate.ownerProfileId()));
    if (request.isEmpty()) {
      return Result.failure(new ApplicationError(
          "SERVICE_REQUEST_NOT_FOUND", "Service request not found: " + command.serviceRequestId()));
    }
    if (!request.get().isCompleted()) {
      return Result.failure(new ApplicationError(
          "INVALID_STATUS_TRANSITION",
          "The service request is not completed",
          "A service intervention report needs a COMPLETED service request; it is " + request.get().status()));
    }
    var from = command.from() != null ? command.from() : request.get().periodStart();
    var to = command.to() != null ? command.to() : request.get().periodEnd();
    return validRange(from, to);
  }

  private static Result<DateRange, ApplicationError> validRange(Instant from, Instant to) {
    try {
      return Result.success(new DateRange(from, to));
    } catch (IllegalArgumentException ex) {
      return Result.failure(ApplicationError.validationError("filters", ex.getMessage()));
    }
  }
}
