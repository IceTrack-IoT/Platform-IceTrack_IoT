package pe.edu.upc.ice.track.platform.reporting.application.internal.commandservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.commandservices.ReportCommandService;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.ReportFileStorage;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportContent;
import pe.edu.upc.ice.track.platform.reporting.application.internal.renderers.ReportRenderer;
import pe.edu.upc.ice.track.platform.reporting.application.internal.strategies.ReportGenerationStrategy;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.RequestReportCommand;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Default implementation of {@link ReportCommandService}.
 *
 * <p>Two Strategy patterns meet here, both resolved from injected maps keyed by bean qualifier:
 * the {@link ReportGenerationStrategy} for {@code command.type().name()} decides <em>what</em> is
 * computed, and the {@link ReportRenderer} for {@code command.format().name()} decides <em>how
 * it is presented</em>. Adding a report type or a format means adding a bean; this class never
 * changes.</p>
 *
 * <p>The pipeline for one request is: compute KPIs, render them to file bytes, upload the file
 * through {@link ReportFileStorage}, and only then mark the report {@code COMPLETED} with the
 * file's reference. If any step fails the report becomes {@code FAILED} and no file reference is
 * kept. The storage key contains a random UUID rather than the report id, which is not known
 * until the report is saved, so the report is saved once, in its final state.</p>
 *
 * <p>Generation is synchronous: the real codebase has no job queue or async infrastructure, so
 * all of the above happens within the request. If saving the report fails after the file was
 * uploaded, the file is deleted again so no orphan is left in the bucket.</p>
 */
@Service
public class ReportCommandServiceImpl implements ReportCommandService {

  private final Map<String, ReportGenerationStrategy> strategies;
  private final Map<String, ReportRenderer> renderers;
  private final ReportFileStorage fileStorage;
  private final ReportRepository reportRepository;
  private final ObjectMapper objectMapper;

  public ReportCommandServiceImpl(
      Map<String, ReportGenerationStrategy> strategies,
      Map<String, ReportRenderer> renderers,
      ReportFileStorage fileStorage,
      ReportRepository reportRepository,
      ObjectMapper objectMapper) {
    this.strategies = strategies;
    this.renderers = renderers;
    this.fileStorage = fileStorage;
    this.reportRepository = reportRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public Report handle(RequestReportCommand command) {
    Objects.requireNonNull(command, "command must not be null");

    var filters = new ReportFilters(
        command.siteId() != null ? new SiteId(command.siteId()) : null,
        command.equipmentId() != null ? new EquipmentId(command.equipmentId()) : null,
        command.technicianId() != null ? new TechnicianId(command.technicianId()) : null,
        new DateRange(command.from(), command.to()));

    var report = new Report(
        command.title(), command.type(), command.format(), new UserId(command.userId()), filters);

    ReportGenerationStrategy strategy = strategies.get(command.type().name());
    if (strategy == null) {
      throw new IllegalStateException("No ReportGenerationStrategy registered for type " + command.type());
    }
    ReportRenderer renderer = renderers.get(command.format().name());
    if (renderer == null) {
      throw new IllegalStateException("No ReportRenderer registered for format " + command.format());
    }

    ReportFile storedFile = null;
    report.markAsGenerating();
    try {
      List<KpiValue> kpis = strategy.generate(filters);
      String payload = writeAsJson(kpis);

      byte[] bytes = renderer.render(
          new ReportContent(command.title(), command.type(), filters, LocalDateTime.now(), kpis));
      String key = "reports/" + command.userId() + "/" + UUID.randomUUID() + "." + renderer.fileExtension();
      fileStorage.store(key, bytes, renderer.contentType());
      storedFile = new ReportFile(key, renderer.contentType(), bytes.length);

      report.markAsCompleted(payload, storedFile);
    } catch (UnsupportedOperationException ex) {
      report.markAsFailed(ex.getMessage());
    } catch (RuntimeException ex) {
      report.markAsFailed("Report generation failed: " + ex.getMessage());
    }

    try {
      return reportRepository.save(report);
    } catch (RuntimeException ex) {
      deleteQuietly(storedFile);
      throw ex;
    }
  }

  private String writeAsJson(List<KpiValue> kpis) {
    try {
      return objectMapper.writeValueAsString(kpis);
    } catch (JacksonException ex) {
      throw new IllegalStateException("Could not serialize report KPIs", ex);
    }
  }

  private void deleteQuietly(ReportFile file) {
    if (file == null) {
      return;
    }
    try {
      fileStorage.delete(file.key());
    } catch (RuntimeException ignored) {
      // Best effort: the original save failure is the error worth surfacing.
    }
  }
}
