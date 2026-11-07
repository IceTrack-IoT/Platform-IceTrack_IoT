package pe.edu.upc.ice.track.platform.reporting.application.internal.eventhandlers;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.profiles.ExternalProfilesServiceForReporting;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.ReportFileStorage;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportContent;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportRenderer;
import pe.edu.upc.ice.track.platform.reporting.application.internal.strategies.InsufficientTelemetryException;
import pe.edu.upc.ice.track.platform.reporting.application.internal.strategies.ReportGenerationStrategy;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.events.ReportRequestedEvent;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFile;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Generates a requested report in the background: computes its KPIs, renders the file, stores it, and
 * records the outcome on the report.
 *
 * <p>Started by {@link ReportRequestedEvent} on the {@code reportTaskExecutor} pool, and only after the
 * request's transaction has committed, so the report it reads is already saved. Each step saves the
 * report, so a status is visible while the work runs:</p>
 * <ul>
 *   <li>{@code PENDING} to {@code GENERATING} when the work starts;</li>
 *   <li>{@code COMPLETED} once the file is stored, with its key, content type, size and completion time;</li>
 *   <li>{@code FAILED} with a reason when the metric cannot be computed (no telemetry), or when the
 *       file cannot be stored after {@value #MAX_STORAGE_ATTEMPTS} attempts, or on any other error.</li>
 * </ul>
 *
 * <p>The file key is {@code reports/{ownerId}/{reportId}.{ext}}. A file that was stored but could not
 * be recorded is deleted again, and so is the partial file of a failed storage.</p>
 *
 * <p>No exception leaves this class: a generation problem is a {@code FAILED} report, never an error
 * for anybody else to handle.</p>
 */
@Service
@Slf4j
public class ReportGenerationWorker {

  /** Attempts at storing the file, the first included. */
  static final int MAX_STORAGE_ATTEMPTS = 3;

  private static final Duration INITIAL_STORAGE_BACKOFF = Duration.ofMillis(500);

  private static final String STORAGE_FAILED_REASON = "Report storage failed";
  private static final String GENERATION_FAILED_REASON = "Report generation failed";

  private final Map<String, ReportGenerationStrategy> strategies;
  private final Map<String, ReportRenderer> renderers;
  private final ReportFileStorage fileStorage;
  private final ReportRepository reportRepository;
  private final ObjectMapper objectMapper;
  private final ExternalProfilesServiceForReporting externalProfilesService;

  public ReportGenerationWorker(
      Map<String, ReportGenerationStrategy> strategies,
      Map<String, ReportRenderer> renderers,
      ReportFileStorage fileStorage,
      ReportRepository reportRepository,
      ObjectMapper objectMapper,
      ExternalProfilesServiceForReporting externalProfilesService) {
    this.strategies = strategies;
    this.renderers = renderers;
    this.fileStorage = fileStorage;
    this.reportRepository = reportRepository;
    this.objectMapper = objectMapper;
    this.externalProfilesService = externalProfilesService;
  }

  /**
   * Handles a newly requested report, after the request's transaction has committed.
   *
   * @param event the event naming the pending report
   */
  @Async("reportTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(ReportRequestedEvent event) {
    generate(event.reportId());
  }

  /**
   * Runs the whole generation for one report. Safe to call for a report that is no longer pending:
   * it does nothing then.
   *
   * @param reportId identifier of the report
   */
  void generate(Long reportId) {
    var report = reportRepository.findById(reportId).orElse(null);
    if (report == null || report.getStatus() != ReportStatus.PENDING) {
      log.info("Report {} is not pending; generation skipped", reportId);
      return;
    }
    report.markAsGenerating();
    reportRepository.save(report);

    String storedKey = null;
    try {
      var ownerId = externalProfilesService.fetchOwnerIdByUserId(report.getUserId().userId())
          .orElseThrow(() -> new IllegalStateException("The requester no longer has an owner profile"));
      var strategy = strategies.get(report.getType().name());
      var renderer = renderers.get(report.getFormat().name());
      if (strategy == null || renderer == null) {
        throw new IllegalStateException("No strategy or renderer registered for " + report.getType() + "/" + report.getFormat());
      }

      List<KpiValue> kpis = strategy.generate(report.getFilters());
      var payload = writeAsJson(kpis);
      var bytes = renderer.render(new ReportContent(
          report.getTitle(), report.getType(), report.getFilters(), LocalDateTime.now(), kpis));

      var key = "reports/%d/%d.%s".formatted(ownerId, report.getId(), renderer.fileExtension());
      storedKey = key;
      storeWithRetries(key, bytes, renderer.contentType());
      report.markAsCompleted(payload, new ReportFile(key, renderer.contentType(), bytes.length));
      log.info("Report {} completed: {} bytes stored at {}", report.getId(), bytes.length, key);
    } catch (InsufficientTelemetryException ex) {
      report.markAsFailed(InsufficientTelemetryException.REASON);
      log.info("Report {} failed: no telemetry in the requested period", report.getId());
    } catch (ReportStorageException ex) {
      log.error("Report {} could not be stored", report.getId(), ex.getCause());
      report.markAsFailed(STORAGE_FAILED_REASON);
      deleteQuietly(storedKey);
    } catch (RuntimeException ex) {
      log.error("Generation of report {} failed", report.getId(), ex);
      report.markAsFailed(GENERATION_FAILED_REASON);
      deleteQuietly(storedKey);
    }

    try {
      reportRepository.save(report);
    } catch (RuntimeException ex) {
      log.error("Report {} could not be recorded; its stored file is removed", report.getId(), ex);
      deleteQuietly(report.getFile() != null ? report.getFile().key() : null);
    }
  }

  /**
   * Stores the file, retrying with exponential backoff.
   *
   * @param key         the object key
   * @param bytes       the file content
   * @param contentType the MIME type
   * @throws ReportStorageException when every attempt failed
   */
  private void storeWithRetries(String key, byte[] bytes, String contentType) {
    for (var attempt = 1; ; attempt++) {
      try {
        fileStorage.store(key, bytes, contentType);
        return;
      } catch (RuntimeException ex) {
        if (attempt >= MAX_STORAGE_ATTEMPTS) {
          throw new ReportStorageException(ex);
        }
        var delay = INITIAL_STORAGE_BACKOFF.multipliedBy(1L << (attempt - 1));
        log.warn("Storing {} failed (attempt {}/{}); retrying in {} ms", key, attempt, MAX_STORAGE_ATTEMPTS, delay.toMillis());
        sleep(delay);
      }
    }
  }

  private static void sleep(Duration delay) {
    try {
      Thread.sleep(delay.toMillis());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ReportStorageException(ex);
    }
  }

  private String writeAsJson(List<KpiValue> kpis) {
    try {
      return objectMapper.writeValueAsString(kpis);
    } catch (JacksonException ex) {
      throw new IllegalStateException("Could not serialize report KPIs", ex);
    }
  }

  private void deleteQuietly(String key) {
    if (key == null) {
      return;
    }
    try {
      fileStorage.delete(key);
    } catch (RuntimeException ignored) {
      // Best effort: the failure already recorded on the report is the one worth keeping.
    }
  }

  /** The file could not be stored, even after retrying. */
  static final class ReportStorageException extends RuntimeException {

    ReportStorageException(Throwable cause) {
      super(cause);
    }
  }
}
