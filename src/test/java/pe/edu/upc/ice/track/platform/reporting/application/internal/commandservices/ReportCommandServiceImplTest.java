package pe.edu.upc.ice.track.platform.reporting.application.internal.commandservices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalAssetsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalProfilesServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalServiceRequestsServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.DeleteReportCommand;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.RequestReportCommand;
import pe.edu.upc.ice.track.platform.reporting.domain.model.events.ReportDeletedEvent;
import pe.edu.upc.ice.track.platform.reporting.domain.model.events.ReportRequestedEvent;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingCommandFixtures.*;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ReportCommandServiceImplTest {

  @Mock private ReportRepository reportRepository;
  @Mock private ExternalProfilesServiceForReporting profiles;
  @Mock private ExternalAssetsServiceForReporting assets;
  @Mock private ExternalServiceRequestsServiceForReporting serviceRequests;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private ReportCommandServiceImpl service;

  private void ownerResolved() {
    doReturn(Optional.of(USER_ID)).when(profiles).fetchUserIdByUsername(USERNAME);
    doReturn(Optional.of(OWNER_ID)).when(profiles).fetchOwnerIdByUserId(USER_ID);
  }

  @Nested
  class RequestReport {

    @Test
    @DisplayName("handle(RequestReportCommand) -> saves a PENDING trimmed report and publishes ReportRequestedEvent (AAA)")
    void happyPath() {
      // Arrange
      ownerResolved();
      doReturn(true).when(assets).isSiteOwnedBy(SITE_ID, OWNER_ID);
      org.mockito.Mockito.doAnswer(inv -> {
        Report report = inv.getArgument(0);
        report.assignId(REPORT_ID);
        return report;
      }).when(reportRepository).save(any(Report.class));

      // Act
      var result = service.handle(uptimeCommand());

      // Assert
      var report = successValue(result);
      assertThat(report.getId()).isEqualTo(REPORT_ID);
      assertThat(report.getTitle()).isEqualTo(TITLE);
      assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
      assertThat(report.getFilters().siteId().siteId()).isEqualTo(SITE_ID);
      assertThat(report.getFilters().dateRange().from()).isEqualTo(FROM);
      assertThat(report.isOwnedBy(new pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId(USER_ID))).isTrue();
      var captor = ArgumentCaptor.forClass(Object.class);
      verify(eventPublisher, times(1)).publishEvent(captor.capture());
      assertThat(captor.getValue()).isEqualTo(new ReportRequestedEvent(REPORT_ID));
      verify(reportRepository, times(1)).save(any(Report.class));
    }

    @Test
    @DisplayName("handle(command with equipment and technician) -> filters carry equipment and technician ids (AAA)")
    void equipmentAndTechnician() {
      // Arrange
      ownerResolved();
      doReturn(true).when(assets).isEquipmentOwnedBy(EQUIPMENT_ID, OWNER_ID);
      org.mockito.Mockito.doAnswer(inv -> {
        Report report = inv.getArgument(0);
        report.assignId(REPORT_ID);
        return report;
      }).when(reportRepository).save(any(Report.class));
      var command = new RequestReportCommand(TITLE, ReportType.TECHNICIAN_PERFORMANCE, ReportFormat.CSV, USERNAME,
          null, EQUIPMENT_ID, TECHNICIAN_ID, FROM, TO);

      // Act
      var report = successValue(service.handle(command));

      // Assert
      assertThat(report.getFilters().equipmentId().equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(report.getFilters().technicianId().technicianId()).isEqualTo(TECHNICIAN_ID);
      assertThat(report.getFilters().siteId()).isNull();
    }

    @Test
    @DisplayName("handle(null) -> NullPointerException, no collaborator touched (AAA)")
    void nullCommand() {
      // Act & Assert
      assertThatThrownBy(() -> service.handle((RequestReportCommand) null))
          .isInstanceOf(NullPointerException.class).hasMessage("command must not be null");
      verifyNoInteractions(reportRepository, profiles, assets, serviceRequests, eventPublisher);
    }

    @Test
    @DisplayName("handle(blank or null title) -> VALIDATION_ERROR on name, nothing saved (AAA)")
    void blankTitle() {
      // Arrange
      var blank = new RequestReportCommand("   ", ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, USERNAME, null, null, null, FROM, TO);
      var nullTitle = new RequestReportCommand(null, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, USERNAME, null, null, null, FROM, TO);

      // Act
      var error = failureError(service.handle(blank));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(error.details()).isEqualTo("The report name is required");
      assertThat(failureError(service.handle(nullTitle)).code()).isEqualTo("VALIDATION_ERROR");
      verifyNoInteractions(reportRepository, profiles, eventPublisher);
    }

    @Test
    @DisplayName("handle(null type or format) -> VALIDATION_ERROR on report (AAA)")
    void nullTypeOrFormat() {
      // Arrange
      var noType = new RequestReportCommand(TITLE, null, ReportFormat.PDF, USERNAME, null, null, null, FROM, TO);
      var noFormat = new RequestReportCommand(TITLE, ReportType.EQUIPMENT_UPTIME, null, USERNAME, null, null, null, FROM, TO);

      // Act & Assert
      assertThat(failureError(service.handle(noType)).code()).isEqualTo("VALIDATION_ERROR");
      assertThat(failureError(service.handle(noFormat)).details())
          .isEqualTo("The report type and format must be one of the values listed in the API documentation");
      verifyNoInteractions(reportRepository, profiles, eventPublisher);
    }

    @Test
    @DisplayName("handle(unknown username) -> ACCESS_DENIED, nothing saved (AAA)")
    void noAccount() {
      // Arrange
      doReturn(Optional.empty()).when(profiles).fetchUserIdByUsername(USERNAME);

      // Act
      var error = failureError(service.handle(uptimeCommand()));

      // Assert
      assertThat(error.code()).isEqualTo("ACCESS_DENIED");
      assertThat(error.details()).isEqualTo("The caller has no platform account");
      verify(profiles, never()).fetchOwnerIdByUserId(any());
      verifyNoInteractions(reportRepository, eventPublisher);
    }

    @Test
    @DisplayName("handle(non-owner caller) -> ACCESS_DENIED, nothing saved (AAA)")
    void notOwner() {
      // Arrange
      doReturn(Optional.of(USER_ID)).when(profiles).fetchUserIdByUsername(USERNAME);
      doReturn(Optional.empty()).when(profiles).fetchOwnerIdByUserId(USER_ID);

      // Act
      var error = failureError(service.handle(uptimeCommand()));

      // Assert
      assertThat(error.code()).isEqualTo("ACCESS_DENIED");
      assertThat(error.details()).isEqualTo("Reports are available to owners only");
      verifyNoInteractions(reportRepository, eventPublisher, assets);
    }

    @Test
    @DisplayName("handle(site not owned) -> SITE_NOT_FOUND, nothing saved (AAA)")
    void siteNotOwned() {
      // Arrange
      ownerResolved();
      doReturn(false).when(assets).isSiteOwnedBy(SITE_ID, OWNER_ID);

      // Act
      var error = failureError(service.handle(uptimeCommand()));

      // Assert
      assertThat(error.code()).isEqualTo("SITE_NOT_FOUND");
      verifyNoInteractions(reportRepository, eventPublisher);
    }

    @Test
    @DisplayName("handle(equipment not owned) -> EQUIPMENT_NOT_FOUND, nothing saved (AAA)")
    void equipmentNotOwned() {
      // Arrange
      ownerResolved();
      doReturn(false).when(assets).isEquipmentOwnedBy(EQUIPMENT_ID, OWNER_ID);
      var command = new RequestReportCommand(TITLE, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, USERNAME,
          null, EQUIPMENT_ID, null, FROM, TO);

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("EQUIPMENT_NOT_FOUND");
      verify(assets, never()).isSiteOwnedBy(any(), any());
      verifyNoInteractions(reportRepository, eventPublisher);
    }

    @Test
    @DisplayName("handle(non-intervention type with serviceRequestId) -> VALIDATION_ERROR on filters.service_request_id (AAA)")
    void serviceRequestIdRefused() {
      // Arrange
      ownerResolved();
      var command = new RequestReportCommand(TITLE, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, USERNAME,
          null, null, null, FROM, TO, REQUEST_ID);

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(error.details()).isEqualTo("service_request_id is only accepted for SERVICE_INTERVENTION reports");
      verifyNoInteractions(reportRepository, eventPublisher, serviceRequests);
    }

    @Test
    @DisplayName("handle(non-intervention type without from/to) -> VALIDATION_ERROR on filters (AAA)")
    void periodRequired() {
      // Arrange
      ownerResolved();
      var noFrom = new RequestReportCommand(TITLE, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, USERNAME, null, null, null, null, TO);
      var noTo = new RequestReportCommand(TITLE, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, USERNAME, null, null, null, FROM, null);

      // Act & Assert
      assertThat(failureError(service.handle(noFrom)).details()).isEqualTo("from and to are required for EQUIPMENT_UPTIME reports");
      assertThat(failureError(service.handle(noTo)).code()).isEqualTo("VALIDATION_ERROR");
      verifyNoInteractions(reportRepository, eventPublisher);
    }

    @Test
    @DisplayName("handle(from after to) -> VALIDATION_ERROR carrying the range message (AAA)")
    void invertedRange() {
      // Arrange
      ownerResolved();
      var command = new RequestReportCommand(TITLE, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, USERNAME, null, null, null, TO, FROM);

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(error.details()).isEqualTo("from must not be after to");
      verifyNoInteractions(reportRepository, eventPublisher);
    }

    @Test
    @DisplayName("handle(non-positive site id) -> VALIDATION_ERROR on report, nothing saved (AAA)")
    void filterConstructionFailure() {
      // Arrange
      ownerResolved();
      var badSite = new RequestReportCommand(TITLE, ReportType.EQUIPMENT_UPTIME, ReportFormat.PDF, USERNAME,
          0L, null, null, FROM, TO);
      doReturn(true).when(assets).isSiteOwnedBy(0L, OWNER_ID);

      // Act
      var error = failureError(service.handle(badSite));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
      verifyNoInteractions(reportRepository, eventPublisher);
    }
  }

  @Nested
  class ServiceIntervention {

    @BeforeEach
    void owner() {
      ownerResolved();
    }

    @Test
    @DisplayName("handle(SERVICE_INTERVENTION without request id) -> VALIDATION_ERROR (AAA)")
    void requestIdRequired() {
      // Arrange
      var command = new RequestReportCommand(TITLE, ReportType.SERVICE_INTERVENTION, ReportFormat.PDF, USERNAME,
          null, null, null, null, null);

      // Act
      var error = failureError(service.handle(command));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
      assertThat(error.details()).isEqualTo("service_request_id is required for SERVICE_INTERVENTION reports");
      verifyNoInteractions(reportRepository, eventPublisher, serviceRequests);
    }

    @Test
    @DisplayName("handle(request unknown) -> SERVICE_REQUEST_NOT_FOUND (AAA)")
    void unknownRequest() {
      // Arrange
      doReturn(Optional.empty()).when(serviceRequests).fetchServiceIntervention(REQUEST_ID);

      // Act
      var error = failureError(service.handle(interventionCommand(null, null)));

      // Assert
      assertThat(error.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      assertThat(error.message()).isEqualTo("Service request not found: 77");
      verifyNoInteractions(reportRepository, eventPublisher);
    }

    @Test
    @DisplayName("handle(request of another owner) -> SERVICE_REQUEST_NOT_FOUND, never leaks existence (AAA)")
    void foreignRequest() {
      // Arrange
      doReturn(Optional.of(intervention(999L, "COMPLETED"))).when(serviceRequests).fetchServiceIntervention(REQUEST_ID);

      // Act
      var error = failureError(service.handle(interventionCommand(null, null)));

      // Assert
      assertThat(error.code()).isEqualTo("SERVICE_REQUEST_NOT_FOUND");
      verifyNoInteractions(reportRepository, eventPublisher);
    }

    @Test
    @DisplayName("handle(request not COMPLETED) -> INVALID_STATUS_TRANSITION naming the current status (AAA)")
    void notCompleted() {
      // Arrange
      doReturn(Optional.of(intervention(OWNER_ID, "IN_PROGRESS"))).when(serviceRequests).fetchServiceIntervention(REQUEST_ID);

      // Act
      var error = failureError(service.handle(interventionCommand(null, null)));

      // Assert
      assertThat(error.code()).isEqualTo("INVALID_STATUS_TRANSITION");
      assertThat(error.message()).isEqualTo("The service request is not completed");
      assertThat(error.details()).isEqualTo("A service intervention report needs a COMPLETED service request; it is IN_PROGRESS");
      verifyNoInteractions(reportRepository, eventPublisher);
    }

    @Test
    @DisplayName("handle(request completed, no from/to) -> period defaults to the request's own (AAA)")
    void defaultsToRequestPeriod() {
      // Arrange
      doReturn(Optional.of(intervention(OWNER_ID, "COMPLETED"))).when(serviceRequests).fetchServiceIntervention(REQUEST_ID);
      org.mockito.Mockito.doAnswer(inv -> {
        Report report = inv.getArgument(0);
        report.assignId(REPORT_ID);
        return report;
      }).when(reportRepository).save(any(Report.class));

      // Act
      var report = successValue(service.handle(interventionCommand(null, null)));

      // Assert
      assertThat(report.getFilters().dateRange().from()).isEqualTo(STARTED);
      assertThat(report.getFilters().dateRange().to()).isEqualTo(COMPLETED);
      assertThat(report.getFilters().serviceRequestId()).isEqualTo(REQUEST_ID);
      verify(eventPublisher, times(1)).publishEvent(new ReportRequestedEvent(REPORT_ID));
    }

    @Test
    @DisplayName("handle(request completed, explicit from/to) -> explicit period wins (AAA)")
    void explicitPeriodWins() {
      // Arrange
      doReturn(Optional.of(intervention(OWNER_ID, "COMPLETED"))).when(serviceRequests).fetchServiceIntervention(REQUEST_ID);
      org.mockito.Mockito.doAnswer(inv -> inv.getArgument(0)).when(reportRepository).save(any(Report.class));

      // Act
      var report = successValue(service.handle(interventionCommand(FROM, TO)));

      // Assert
      assertThat(report.getFilters().dateRange().from()).isEqualTo(FROM);
      assertThat(report.getFilters().dateRange().to()).isEqualTo(TO);
    }

    @Test
    @DisplayName("handle(explicit from after the request's end) -> VALIDATION_ERROR on filters (AAA)")
    void mixedInvalidRange() {
      // Arrange
      doReturn(Optional.of(intervention(OWNER_ID, "COMPLETED"))).when(serviceRequests).fetchServiceIntervention(REQUEST_ID);
      var from = COMPLETED.plusSeconds(60);

      // Act
      var error = failureError(service.handle(interventionCommand(from, null)));

      // Assert
      assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
      verifyNoInteractions(reportRepository, eventPublisher);
    }
  }

  @Nested
  class DeleteReport {

    @Test
    @DisplayName("handle(DeleteReportCommand) on a COMPLETED report -> deletes, publishes ReportDeletedEvent with file key (AAA)")
    void deletesCompleted() {
      // Arrange
      doReturn(Optional.of(USER_ID)).when(profiles).fetchUserIdByUsername(USERNAME);
      doReturn(Optional.of(completedReport())).when(reportRepository).findById(REPORT_ID);

      // Act
      var result = service.handle(new DeleteReportCommand(REPORT_ID, USERNAME));

      // Assert
      assertThat(successValue(result)).isEqualTo(REPORT_ID);
      verify(reportRepository, times(1)).deleteById(REPORT_ID);
      verify(eventPublisher, times(1)).publishEvent(new ReportDeletedEvent(REPORT_ID, FILE_KEY));
    }

    @Test
    @DisplayName("handle(DeleteReportCommand) on a FAILED report -> deletes, event carries a null file key (AAA)")
    void deletesFailed() {
      // Arrange
      doReturn(Optional.of(USER_ID)).when(profiles).fetchUserIdByUsername(USERNAME);
      doReturn(Optional.of(failedReport())).when(reportRepository).findById(REPORT_ID);

      // Act
      var result = service.handle(new DeleteReportCommand(REPORT_ID, USERNAME));

      // Assert
      assertThat(successValue(result)).isEqualTo(REPORT_ID);
      verify(eventPublisher, times(1)).publishEvent(new ReportDeletedEvent(REPORT_ID, null));
    }

    @Test
    @DisplayName("handle(DeleteReportCommand) with unknown username -> REPORT_NOT_FOUND, repository untouched (AAA)")
    void unknownUser() {
      // Arrange
      doReturn(Optional.empty()).when(profiles).fetchUserIdByUsername(USERNAME);

      // Act
      var error = failureError(service.handle(new DeleteReportCommand(REPORT_ID, USERNAME)));

      // Assert
      assertThat(error.code()).isEqualTo("REPORT_NOT_FOUND");
      verifyNoInteractions(reportRepository, eventPublisher);
    }

    @Test
    @DisplayName("handle(DeleteReportCommand) on a missing report -> REPORT_NOT_FOUND (AAA)")
    void missingReport() {
      // Arrange
      doReturn(Optional.of(USER_ID)).when(profiles).fetchUserIdByUsername(USERNAME);
      doReturn(Optional.empty()).when(reportRepository).findById(REPORT_ID);

      // Act
      var error = failureError(service.handle(new DeleteReportCommand(REPORT_ID, USERNAME)));

      // Assert
      assertThat(error.code()).isEqualTo("REPORT_NOT_FOUND");
      assertThat(error.message()).isEqualTo("Report not found: 500");
      verify(reportRepository, never()).deleteById(any());
      verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("handle(DeleteReportCommand) on someone else's report -> REPORT_NOT_FOUND (AAA)")
    void foreignReport() {
      // Arrange
      doReturn(Optional.of(OTHER_USER_ID)).when(profiles).fetchUserIdByUsername(USERNAME);
      doReturn(Optional.of(completedReport())).when(reportRepository).findById(REPORT_ID);

      // Act
      var error = failureError(service.handle(new DeleteReportCommand(REPORT_ID, USERNAME)));

      // Assert
      assertThat(error.code()).isEqualTo("REPORT_NOT_FOUND");
      verify(reportRepository, never()).deleteById(any());
      verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("handle(DeleteReportCommand) on PENDING / GENERATING -> INVALID_STATUS_TRANSITION with the status (AAA)")
    void notDeletable() {
      // Arrange
      doReturn(Optional.of(USER_ID)).when(profiles).fetchUserIdByUsername(USERNAME);
      doReturn(Optional.of(generatingReport())).when(reportRepository).findById(REPORT_ID);

      // Act
      var error = failureError(service.handle(new DeleteReportCommand(REPORT_ID, USERNAME)));

      // Assert
      assertThat(error.code()).isEqualTo("INVALID_STATUS_TRANSITION");
      assertThat(error.message()).isEqualTo("The report is still being generated");
      assertThat(error.details()).isEqualTo("A report can be deleted once it is COMPLETED or FAILED; it is GENERATING");
      verify(reportRepository, never()).deleteById(any());
      verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("handle((DeleteReportCommand) null) -> NullPointerException (AAA)")
    void nullDelete() {
      // Act & Assert
      assertThatThrownBy(() -> service.handle((DeleteReportCommand) null)).isInstanceOf(NullPointerException.class);
      verifyNoMoreInteractions(reportRepository);
    }
  }
}
