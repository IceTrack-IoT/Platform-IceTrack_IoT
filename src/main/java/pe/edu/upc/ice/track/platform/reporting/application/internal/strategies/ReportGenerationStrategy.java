package pe.edu.upc.ice.track.platform.reporting.application.internal.strategies;

import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFilters;

import java.util.List;

/**
 * Strategy pattern contract for computing the KPIs of a {@code Report}.
 *
 * <p>Each {@link pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType}
 * is realized by exactly one {@code @Service}-annotated implementation of this interface,
 * registered under a Spring bean qualifier equal to the report type's enum name (e.g.
 * {@code "EQUIPMENT_UPTIME"}). {@code ReportCommandServiceImpl} resolves the strategy to run by
 * looking up {@code report.getType().name()} in the injected
 * {@code Map<String, ReportGenerationStrategy>}, so adding a new report type only requires a new
 * enum constant and a new {@code @Service} bean -- the command service itself never changes.</p>
 */
public interface ReportGenerationStrategy {

  /**
   * Computes the KPIs for a report, given the filters the requester supplied.
   *
   * @param filters the scope (site, unit, technician, date range) to compute over; required
   * @return the KPIs that make up the report's result, in display order
   */
  List<KpiValue> generate(ReportFilters filters);
}
