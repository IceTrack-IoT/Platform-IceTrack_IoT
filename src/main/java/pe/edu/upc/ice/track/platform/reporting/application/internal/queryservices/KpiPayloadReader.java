package pe.edu.upc.ice.track.platform.reporting.application.internal.queryservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.KpiValue;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads back the KPIs a completed report stored in its {@code resultPayload}, the reverse of what
 * {@code ReportGenerationWorker} writes.
 *
 * <p>A payload stored before KPIs carried a key still reads: {@link KpiValue} derives the missing
 * key from the label. A payload that cannot be read at all is logged and reported as absent rather
 * than failing the read of the whole report, which still carries the raw payload.</p>
 */
@Component
@Slf4j
public class KpiPayloadReader {

  private final ObjectMapper objectMapper;

  public KpiPayloadReader(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Reads the KPIs of a report.
   *
   * @param report the report
   * @return the KPIs in the order the strategy produced them; empty when the report is not
   *         {@code COMPLETED} or its payload cannot be read
   */
  public Optional<List<KpiValue>> read(Report report) {
    if (report == null || report.getStatus() != ReportStatus.COMPLETED || report.getResultPayload() == null) {
      return Optional.empty();
    }
    try {
      var values = objectMapper.readValue(report.getResultPayload(), KpiValue[].class);
      return Optional.of(values == null
          ? List.of()
          : Arrays.stream(values).filter(Objects::nonNull).toList());
    } catch (JacksonException ex) {
      // Also covers a KPI the record refuses, such as one without a label: Jackson wraps a failing
      // constructor in a JacksonException.
      log.warn("The KPI payload of report {} could not be read: {}", report.getId(), ex.getMessage());
      return Optional.empty();
    }
  }
}
