package pe.edu.upc.ice.track.platform.reporting.application.internal.eventhandlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.acl.ExternalProfilesServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.ReportFileStorage;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportContent;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportRenderer;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportSection;
import pe.edu.upc.ice.track.platform.reporting.application.internal.strategies.InsufficientTelemetryException;
import pe.edu.upc.ice.track.platform.reporting.application.internal.strategies.ReportGenerationStrategy;
import pe.edu.upc.ice.track.platform.reporting.domain.model.events.ReportRequestedEvent;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;
import pe.edu.upc.ice.track.platform.reporting.interfaces.events.ReportStatusChangedIntegrationEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static pe.edu.upc.ice.track.platform.reporting.test.fixtures.ReportingDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ReportGenerationWorkerTest {

  private static final byte[] BYTES = {1, 2, 3, 4};
  private static final String KEY = "reports/5/500.pdf";

  @Mock private ReportGenerationStrategy strategy;
  @Mock private ReportRenderer renderer;
  @Mock private ReportFileStorage fileStorage;
  @Mock private ReportRepository reportRepository;
  @Mock private ObjectMapper objectMapper;
  @Mock private ExternalProfilesServiceForReporting profiles;
  @Mock private ApplicationEventPublisher eventPublisher;

  private ReportGenerationWorker worker;

  @BeforeEach
  void setUp() {
    Map<String, ReportGenerationStrategy> strategies = new HashMap<>();
    strategies.put("EQUIPMENT_UPTIME", strategy);
    Map<String, ReportRenderer> renderers = new HashMap<>();
    renderers.put("PDF", renderer);
    worker = new ReportGenerationWorker(strategies, renderers, fileStorage, reportRepository, objectMapper, profiles, eventPublisher);
  }

  private void pendingReportStored(Report report) {
    doReturn(Optional.of(report)).when(reportRepository).findById(REPORT_ID);
  }

  private void ownerKnown() {
    doReturn(Optional.of(OWNER_ID)).when(profiles).fetchOwnerIdByUserId(USER_ID);
  }

  private void rendering() {
    doReturn(sampleKpis()).when(strategy).generate(any());
    doReturn(List.of(new ReportSection("S", List.of()))).when(strategy).sections(any());
    doReturn("[kpis]").when(objectMapper).writeValueAsString(any());
    doReturn(BYTES).when(renderer).render(any());
    doReturn("application/pdf").when(renderer).contentType();
    doReturn("pdf").when(renderer).fileExtension();
  }

  private List<ReportStatusChangedIntegrationEvent> announced(int expected) {
    var captor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher, times(expected)).publishEvent(captor.capture());
    return captor.getAllValues().stream().map(ReportStatusChangedIntegrationEvent.class::cast).toList();
  }

  @Nested
  class Success {

    @Test
    @DisplayName("generate(id) -> report COMPLETED with stored file, payload and both status announcements (AAA)")
    void completes() {
      // Arrange
      var report = pendingReport();
      pendingReportStored(report);
      ownerKnown();
      rendering();

      // Act
      worker.generate(REPORT_ID);

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED);
      assertThat(report.getResultPayload()).isEqualTo("[kpis]");
      assertThat(report.getFile().key()).isEqualTo(KEY);
      assertThat(report.getFile().contentType()).isEqualTo("application/pdf");
      assertThat(report.getFile().sizeBytes()).isEqualTo(4L);
      verify(fileStorage, times(1)).store(KEY, BYTES, "application/pdf");
      verify(reportRepository, times(2)).save(report);
      var events = announced(2);
      assertThat(events.get(0).previousStatus()).isEqualTo("PENDING");
      assertThat(events.get(0).status()).isEqualTo("GENERATING");
      assertThat(events.get(1).previousStatus()).isEqualTo("GENERATING");
      assertThat(events.get(1).status()).isEqualTo("COMPLETED");
      assertThat(events.get(1).ownerProfileId()).isEqualTo(OWNER_ID);
      assertThat(events.get(1).reportId()).isEqualTo(REPORT_ID);
    }

    @Test
    @DisplayName("generate(id) -> renderer receives title, type, filters, KPIs and the strategy's sections (AAA)")
    void rendererContent() {
      // Arrange
      var report = pendingReport();
      pendingReportStored(report);
      ownerKnown();
      rendering();

      // Act
      worker.generate(REPORT_ID);

      // Assert
      var captor = ArgumentCaptor.forClass(ReportContent.class);
      verify(renderer, times(1)).render(captor.capture());
      var content = captor.getValue();
      assertThat(content.title()).isEqualTo(TITLE);
      assertThat(content.type()).isEqualTo(report.getType());
      assertThat(content.filters()).isEqualTo(report.getFilters());
      assertThat(content.kpis()).isEqualTo(sampleKpis());
      assertThat(content.sections()).extracting(ReportSection::title).containsExactly("S");
      assertThat(content.generatedAt()).isNotNull();
    }

    @Test
    @DisplayName("on(ReportRequestedEvent) -> delegates to generate (AAA)")
    void listenerDelegates() {
      // Arrange
      var report = pendingReport();
      pendingReportStored(report);
      ownerKnown();
      rendering();

      // Act
      worker.on(new ReportRequestedEvent(REPORT_ID));

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED);
    }

    @Test
    @DisplayName("generate(id) with two storage failures then success -> still COMPLETED after retrying (AAA)")
    void storageRecoversOnThirdAttempt() {
      // Arrange
      var report = pendingReport();
      pendingReportStored(report);
      ownerKnown();
      rendering();
      doThrow(new RuntimeException("net")).doThrow(new RuntimeException("net")).doNothing()
          .when(fileStorage).store(anyString(), any(), anyString());

      // Act
      worker.generate(REPORT_ID);

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETED);
      verify(fileStorage, times(3)).store(KEY, BYTES, "application/pdf");
      verify(fileStorage, never()).delete(anyString());
    }
  }

  @Nested
  class Skipped {

    @Test
    @DisplayName("generate(id) for a missing report -> nothing happens (AAA)")
    void missing() {
      // Arrange
      doReturn(Optional.empty()).when(reportRepository).findById(REPORT_ID);

      // Act
      worker.generate(REPORT_ID);

      // Assert
      verify(reportRepository, never()).save(any());
      verifyNoInteractions(profiles, strategy, renderer, fileStorage, eventPublisher);
    }

    @Test
    @DisplayName("generate(id) for a report that is not PENDING -> skipped, state untouched (AAA)")
    void notPending() {
      // Arrange
      var report = generatingReport();
      pendingReportStored(report);

      // Act
      worker.generate(REPORT_ID);

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.GENERATING);
      verify(reportRepository, never()).save(any());
      verifyNoInteractions(profiles, strategy, renderer, fileStorage, eventPublisher);
    }
  }

  @Nested
  class Failures {

    @Test
    @DisplayName("generate(id) when the owner profile is gone -> FAILED 'Report generation failed', no announcements (AAA)")
    void noOwnerProfile() {
      // Arrange
      var report = pendingReport();
      pendingReportStored(report);
      doReturn(Optional.empty()).when(profiles).fetchOwnerIdByUserId(USER_ID);

      // Act
      worker.generate(REPORT_ID);

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
      assertThat(report.getResultPayload()).isEqualTo("Report generation failed");
      verify(reportRepository, times(2)).save(report);
      verifyNoInteractions(strategy, renderer, eventPublisher);
      verify(fileStorage, never()).store(any(), any(), any());
    }

    @Test
    @DisplayName("generate(id) with no strategy for the type -> FAILED 'Report generation failed' (AAA)")
    void noStrategy() {
      // Arrange
      var report = Report.rehydrate(REPORT_ID, TITLE, pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType.TEMPERATURE_EXCURSION,
          pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat.PDF, ReportStatus.PENDING, null,
          new pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId(USER_ID), siteFilters(),
          java.time.Instant.now(), null, null);
      pendingReportStored(report);
      ownerKnown();

      // Act
      worker.generate(REPORT_ID);

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
      assertThat(report.getResultPayload()).isEqualTo("Report generation failed");
      verify(fileStorage, never()).store(any(), any(), any());
      assertThat(announced(2).get(1).status()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("generate(id) with no renderer for the format -> FAILED 'Report generation failed' (AAA)")
    void noRenderer() {
      // Arrange
      var report = Report.rehydrate(REPORT_ID, TITLE, pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType.EQUIPMENT_UPTIME,
          pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat.XLSX, ReportStatus.PENDING, null,
          new pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId(USER_ID), siteFilters(),
          java.time.Instant.now(), null, null);
      pendingReportStored(report);
      ownerKnown();

      // Act
      worker.generate(REPORT_ID);

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
      verifyNoInteractions(strategy);
    }

    @Test
    @DisplayName("generate(id) with InsufficientTelemetryException -> FAILED with the telemetry reason, nothing stored (AAA)")
    void insufficientTelemetry() {
      // Arrange
      var report = pendingReport();
      pendingReportStored(report);
      ownerKnown();
      doThrow(new InsufficientTelemetryException()).when(strategy).generate(any());

      // Act
      worker.generate(REPORT_ID);

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
      assertThat(report.getResultPayload()).isEqualTo("Insufficient telemetry data for the period");
      verify(fileStorage, never()).store(any(), any(), any());
      verify(fileStorage, never()).delete(any());
    }

    @Test
    @DisplayName("generate(id) with an unexpected strategy error -> FAILED 'Report generation failed' (AAA)")
    void unexpectedError() {
      // Arrange
      var report = pendingReport();
      pendingReportStored(report);
      ownerKnown();
      doThrow(new IllegalStateException("boom")).when(strategy).generate(any());

      // Act
      worker.generate(REPORT_ID);

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
      assertThat(report.getResultPayload()).isEqualTo("Report generation failed");
      assertThat(announced(2).get(1).status()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("generate(id) when KPIs cannot be serialized -> FAILED 'Report generation failed' (AAA)")
    void serializationFails() {
      // Arrange
      var report = pendingReport();
      pendingReportStored(report);
      ownerKnown();
      doReturn(sampleKpis()).when(strategy).generate(any());
      doReturn(List.of()).when(strategy).sections(any());
      doThrow(mock(JacksonException.class)).when(objectMapper).writeValueAsString(any());

      // Act
      worker.generate(REPORT_ID);

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
      assertThat(report.getResultPayload()).isEqualTo("Report generation failed");
      verifyNoInteractions(renderer);
    }

    @Test
    @DisplayName("generate(id) with a failing renderer -> FAILED, no file to clean (AAA)")
    void rendererFails() {
      // Arrange
      var report = pendingReport();
      pendingReportStored(report);
      ownerKnown();
      doReturn(sampleKpis()).when(strategy).generate(any());
      doReturn(List.of()).when(strategy).sections(any());
      doReturn("[]").when(objectMapper).writeValueAsString(any());
      doThrow(new IllegalStateException("render")).when(renderer).render(any());

      // Act
      worker.generate(REPORT_ID);

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
      verify(fileStorage, never()).delete(any());
    }

    @Test
    @DisplayName("generate(id) when storage fails 3 times -> FAILED 'Report storage failed' and the key is cleaned (AAA)")
    void storageFails() {
      // Arrange
      var report = pendingReport();
      pendingReportStored(report);
      ownerKnown();
      rendering();
      doThrow(new RuntimeException("net")).when(fileStorage).store(anyString(), any(), anyString());

      // Act
      worker.generate(REPORT_ID);

      // Assert
      assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
      assertThat(report.getResultPayload()).isEqualTo("Report storage failed");
      verify(fileStorage, times(3)).store(KEY, BYTES, "application/pdf");
      verify(fileStorage, times(1)).delete(KEY);
    }

    @Test
    @DisplayName("generate(id) when cleanup after a failure also fails -> failure still recorded (AAA)")
    void cleanupFailureSwallowed() {
      // Arrange
      var report = pendingReport();
      pendingReportStored(report);
      ownerKnown();
      rendering();
      doThrow(new RuntimeException("net")).when(fileStorage).store(anyString(), any(), anyString());
      doThrow(new RuntimeException("gone")).when(fileStorage).delete(KEY);

      // Act
      worker.generate(REPORT_ID);

      // Assert
      assertThat(report.getResultPayload()).isEqualTo("Report storage failed");
      verify(reportRepository, times(2)).save(report);
    }

    @Test
    @DisplayName("generate(id) when the final save fails -> the stored file is removed and nothing is announced as done (AAA)")
    void finalSaveFails() {
      // Arrange
      var report = pendingReport();
      pendingReportStored(report);
      ownerKnown();
      rendering();
      doAnswer(inv -> inv.getArgument(0)).doThrow(new RuntimeException("db")).when(reportRepository).save(any(Report.class));

      // Act
      worker.generate(REPORT_ID);

      // Assert
      verify(fileStorage, times(1)).delete(KEY);
      var events = announced(1);
      assertThat(events.get(0).status()).isEqualTo("GENERATING");
    }
  }
}
