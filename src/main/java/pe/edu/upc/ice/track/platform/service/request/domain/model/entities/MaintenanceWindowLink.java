package pe.edu.upc.ice.track.platform.service.request.domain.model.entities;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.MaintenanceOrigin;

import java.time.Instant;
import java.util.Objects;

/**
 * The trace, kept on a {@code ServiceRequest}, of a maintenance window opened on its unit while the
 * work was in progress.
 *
 * <p>The window itself belongs to Monitoring and Alerting; this entity only records what the order
 * needs to show which windows the intervention involved: the window's identity, origin, technician,
 * start, and - once the window is closed - its end and closure reason. It is identified within its
 * request by {@code windowId}, which is what keeps a duplicated event from linking the same window
 * twice.</p>
 *
 * <p>No JPA or persistence annotation is present here - the mapping lives exclusively in
 * {@code MaintenanceWindowLinkPersistenceEntity}.</p>
 */
@Getter
public class MaintenanceWindowLink {

  /** Maximum number of characters a closure reason may hold, matching the column width. */
  public static final int MAX_CLOSURE_REASON_LENGTH = 30;

  private final Long id;
  private final Long windowId;
  private final MaintenanceOrigin origin;
  private final Long technicianProfileId;
  private final Instant startedAt;
  private Instant endedAt;
  private String closureReason;

  /**
   * Reconstitutes a link, typically from persistence.
   *
   * @param id                  the persistence identity, or {@code null} for a link not yet persisted
   * @param windowId            identity of the window in Monitoring and Alerting; required
   * @param origin              how the window was started; required
   * @param technicianProfileId the technician attributed to the window, may be {@code null}
   * @param startedAt           when the window started; required
   * @param endedAt             when it ended, or {@code null} while open
   * @param closureReason       why it ended, or {@code null} while open
   * @throws IllegalArgumentException when a required value is missing or the closure is inconsistent
   */
  public MaintenanceWindowLink(Long id, Long windowId, MaintenanceOrigin origin, Long technicianProfileId,
                               Instant startedAt, Instant endedAt, String closureReason) {
    if (windowId == null || origin == null || startedAt == null) {
      throw new IllegalArgumentException("A maintenance window link needs the window, its origin and its start");
    }
    this.id = id;
    this.windowId = windowId;
    this.origin = origin;
    this.technicianProfileId = technicianProfileId;
    this.startedAt = startedAt;
    this.endedAt = endedAt;
    this.closureReason = normalizedReason(closureReason);
  }

  /**
   * Records a new, not yet persisted, link to an open window.
   *
   * @param windowId            identity of the window; required
   * @param origin              how the window was started; required
   * @param technicianProfileId the technician attributed to the window, may be {@code null}
   * @param startedAt           when the window started; required
   * @return the link
   */
  public static MaintenanceWindowLink open(Long windowId, MaintenanceOrigin origin, Long technicianProfileId,
                                           Instant startedAt) {
    return new MaintenanceWindowLink(null, windowId, origin, technicianProfileId, startedAt, null, null);
  }

  /**
   * Completes the link with the end of its window. Idempotent: a link already completed keeps its
   * first end and reason.
   *
   * @param endedAt       when the window ended; required
   * @param closureReason why it ended; required
   * @return {@code true} when this call completed the link
   */
  public boolean close(Instant endedAt, String closureReason) {
    Objects.requireNonNull(endedAt, "endedAt must not be null");
    if (this.endedAt != null) {
      return false;
    }
    var reason = normalizedReason(closureReason);
    if (reason == null) {
      throw new IllegalArgumentException("A closure reason is required to complete a maintenance window link");
    }
    this.endedAt = endedAt.isBefore(startedAt) ? startedAt : endedAt;
    this.closureReason = reason;
    return true;
  }

  /**
   * @param candidateWindowId the window identity to test
   * @return {@code true} when this link traces that window
   */
  public boolean isFor(Long candidateWindowId) {
    return windowId.equals(candidateWindowId);
  }

  private static String normalizedReason(String reason) {
    if (reason == null || reason.isBlank()) {
      return null;
    }
    var trimmed = reason.strip();
    if (trimmed.length() > MAX_CLOSURE_REASON_LENGTH) {
      throw new IllegalArgumentException(
          "A closure reason must not exceed %d characters".formatted(MAX_CLOSURE_REASON_LENGTH));
    }
    return trimmed;
  }
}
