package pe.edu.upc.ice.track.platform.reporting.application.internal.commandservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.commandservices.ReportCommandService;
import pe.edu.upc.ice.track.platform.reporting.application.internal.strategies.ReportGenerationStrategy;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.RequestReportCommand;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.reporting.domain.repositories.ReportRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of {@link ReportCommandService}.
 *
 * <p>The matching {@link ReportGenerationStrategy} is resolved from the injected
 * {@code Map<String, ReportGenerationStrategy>} -- which Spring populates with every
 * {@code ReportGenerationStrategy} bean keyed by its {@code @Service} qualifier -- by looking up
 * {@code command.type().name()}. This is the Strategy pattern's selection point: adding a new
 * {@link ReportType} only requires a new strategy bean qualified with the new enum's name; this
 * class never changes.</p>
 *
 * <p>Generation is synchronous: since the real codebase has no job queue or async
 * infrastructure, {@code markAsGenerating}, the strategy call, and {@code markAsCompleted}/
 * {@code markAsFailed} all happen in-memory within the same request, with the aggregate saved
 * once at the end in its final state. This is a deliberate simplification over a real async
 * pipeline, documented in the module's README.</p>
 */
@Service
public class ReportCommandServiceImpl implements ReportCommandService {

  private final Map<String, ReportGenerationStrategy> strategies;
  private final ReportRepository reportRepository;
  private final ObjectMapper objectMapper;

  public ReportCommandServiceImpl(
      Map<String, ReportGenerationStrategy> strategies,
      ReportRepository reportRepository,
      ObjectMapper objectMapper) {
    this.strategies = strategies;
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

    report.markAsGenerating();
    try {
      List<KpiValue> kpis = strategy.generate(filters);
      report.markAsCompleted(writeAsJson(kpis));
    } catch (UnsupportedOperationException ex) {
      report.markAsFailed(ex.getMessage());
    } catch (RuntimeException ex) {
      report.markAsFailed("Report generation failed: " + ex.getMessage());
    }

    return reportRepository.save(report);
  }

  private String writeAsJson(List<KpiValue> kpis) {
    try {
      return objectMapper.writeValueAsString(kpis);
    } catch (JacksonException ex) {
      throw new IllegalStateException("Could not serialize report KPIs", ex);
    }
  }
}