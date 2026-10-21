package pe.edu.upc.ice.track.platform.reporting.interfaces.acl;

/**
 * Anti-Corruption Layer facade exposing {@code reporting} to other bounded contexts (e.g.
 * {@code profiles}'s {@code DashboardConfig}, which may want a quick KPI number without
 * requesting and polling a full, persisted {@link
 * pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report}).
 *
 * <p>As with every other ACL facade in this codebase, every parameter and return type here is a
 * primitive, a boxed primitive, or a {@code String} -- never a {@code reporting} aggregate, value
 * object, command or repository.</p>
 */
public interface ReportingContextFacade {

  /**
   * Returns the count of reports a user has requested in total, regardless of status. Intended
   * for a lightweight dashboard tile rather than a full report listing.
   *
   * @param userId identifier of the user
   * @return how many reports the user has requested
   */
  int countReportsByUser(Long userId);

  /**
   * Returns the status of the most recently requested report for a user, or {@code null} when
   * the user has never requested one.
   *
   * @param userId identifier of the user
   * @return the latest report's status name (e.g. {@code "COMPLETED"}), or {@code null}
   */
  String fetchLatestReportStatusByUser(Long userId);
}
