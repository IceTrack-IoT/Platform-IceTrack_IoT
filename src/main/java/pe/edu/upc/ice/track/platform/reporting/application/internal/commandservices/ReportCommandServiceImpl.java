package pe.edu.upc.ice.track.platform.reporting.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.reporting.application.commandservices.ReportCommandService;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalAssetsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalProfilesServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.RequestReportCommand;
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
 */
@Service
@Slf4j
public class ReportCommandServiceImpl implements ReportCommandService {

  private final ReportRepository reportRepository;
  private final ExternalProfilesServiceForReporting externalProfilesService;
  private final ExternalAssetsServiceForReporting externalAssetsService;
  private final ApplicationEventPublisher eventPublisher;

  public ReportCommandServiceImpl(
      ReportRepository reportRepository,
      ExternalProfilesServiceForReporting externalProfilesService,
      ExternalAssetsServiceForReporting externalAssetsService,
      ApplicationEventPublisher eventPublisher) {
    this.reportRepository = reportRepository;
    this.externalProfilesService = externalProfilesService;
    this.externalAssetsService = externalAssetsService;
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

    try {
      var filters = new ReportFilters(
          command.siteId() != null ? new SiteId(command.siteId()) : null,
          command.equipmentId() != null ? new EquipmentId(command.equipmentId()) : null,
          command.technicianId() != null ? new TechnicianId(command.technicianId()) : null,
          new DateRange(command.from(), command.to()));
      var saved = reportRepository.save(new Report(
          command.title().trim(), command.type(), command.format(), new UserId(userId.get()), filters));
      eventPublisher.publishEvent(new ReportRequestedEvent(saved.getId()));
      log.info("Report {} requested as {} ({})", saved.getId(), command.type(), command.format());
      return Result.success(saved);
    } catch (IllegalArgumentException | NullPointerException ex) {
      return Result.failure(ApplicationError.validationError("report", ex.getMessage()));
    }
  }
}
