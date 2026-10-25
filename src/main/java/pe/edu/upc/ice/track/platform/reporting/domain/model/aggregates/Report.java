package pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Report aggregate root of the {@code reporting} bounded context.
 *
 * <p>Owns the lifecycle of a single requested report, from the moment it is asked for
 * ({@code PENDING}) through its computation ({@code GENERATING}) to either
 * {@code COMPLETED} (with {@code resultPayload} holding the JSON serialization of the
 * {@code List<KpiValue>} its {@code ReportGenerationStrategy} produced) or {@code FAILED} (with
 * {@code resultPayload} holding the failure reason instead). This context computes KPIs by
 * reading other Bounded Contexts through outbound ports; it never persists their data, only the
 * request and its outcome.</p>
 *
 * <p>{@code filters} is a {@link ReportFilters} Value Object, embedded by composition: it has no
 * identity or lifecycle of its own and is never shared between two reports. {@code userId} reuses
 * the {@code shared} Value Object the same way {@code Profile} and {@code DashboardConfig}
 * already do, rather than a bare {@code Long}, since the identifier itself has no other meaning
 * in this context.</p>
 *
 * <p>No JPA or persistence annotation is present here -- the mapping lives exclusively in
 * {@code ReportPersistenceEntity}.</p>
 */
@Getter
public class Report extends AbstractDomainAggregateRoot<Report> {

  private Long id;
  private final String title;
  private final ReportType type;
  private final ReportFormat format;
  private ReportStatus status;
  private String resultPayload;
  private final UserId userId;
  private final ReportFilters filters;
  private LocalDateTime requestedAt;
  private LocalDate completedAt;
  private ReportFile file;

  public Report(String title, ReportType type, ReportFormat format, UserId userId, ReportFilters filters) {
    this.title = Objects.requireNonNull(title, "title must not be null");
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.format = Objects.requireNonNull(format, "format must not be null");
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.filters = Objects.requireNonNull(filters, "filters must not be null");
    this.status = ReportStatus.PENDING;
    this.requestedAt = LocalDateTime.now();
  }

  /**
   * Restores a report exactly as persisted, including its current status and timestamps.
   *
   * <p>A package-private factory rather than reflection, the same pattern used across this
   * platform's other aggregates: the persistence assembler is the only caller, and it is
   * responsible for handing back every field it read from the row.</p>
   */
  public static Report rehydrate(
      Long id, String title, ReportType type, ReportFormat format, ReportStatus status,
      String resultPayload, UserId userId, ReportFilters filters, LocalDateTime requestedAt,
      LocalDate completedAt, ReportFile file) {
    var report = new Report(title, type, format, userId, filters);
    report.id = id;
    report.status = status;
    report.resultPayload = resultPayload;
    report.requestedAt = requestedAt;
    report.completedAt = completedAt;
    report.file =file;
    return report;
  }

  public void assignId(Long id) {
    this.id = id;
  }

  /** Transitions a freshly requested report into computation. */
  public void markAsGenerating() {
    if (status != ReportStatus.PENDING) {
      throw new IllegalStateException("Only a PENDING report can start generating, was " + status);
    }
    this.status = ReportStatus.GENERATING;
  }

  /**
   * Records a successful computation.
   *
   * @param payload the JSON serialization of the KPI values the strategy produced; required
   */
  public void markAsCompleted(String payload,ReportFile file) {
    if (status != ReportStatus.GENERATING) {
      throw new IllegalStateException("Only a GENERATING report can be completed, was " + status);
    }
    this.resultPayload = Objects.requireNonNull(payload, "payload must not be null");
    this.file = Objects.requireNonNull(file, "file must not be null");
    this.status = ReportStatus.COMPLETED;
    this.completedAt = LocalDate.now();
  }

  /**
   * Records a failed computation.
   *
   * @param reason a human-readable explanation of what went wrong; required
   */
  public void markAsFailed(String reason) {
    if (status != ReportStatus.GENERATING) {
      throw new IllegalStateException("Only a GENERATING report can be failed, was " + status);
    }
    this.resultPayload = Objects.requireNonNull(reason, "reason must not be null");
    this.status = ReportStatus.FAILED;
    this.completedAt = LocalDate.now();
  }

  /**
   * Whether a downloadable file exists for this report.
   *
   * @return {@code true} only for a {@code COMPLETED} report with a stored file
   */
  public boolean hasFile() {
    return status == ReportStatus.COMPLETED && file != null;
  }

  /**
   * Whether the given user is the one who requested this report.
   *
   * @param candidate the user to check
   * @return {@code true} if {@code candidate} is the requester
   */
  public boolean isOwnedBy(UserId candidate) {
    return userId.equals(candidate);
  }

}
