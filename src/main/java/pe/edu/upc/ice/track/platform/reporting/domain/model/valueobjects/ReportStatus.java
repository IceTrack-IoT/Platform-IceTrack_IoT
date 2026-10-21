package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

/**
 *  Enumeration representing the lifecycle state of a {@code Report}.
 *
 *  <p>{@code PENDING -> GENERATING -> COMPLETED} or {@code GENERATING -> FAILED}; see
 *  {@code Report.markAsGenerating()}/{@code markAsCompleted(String)}/{@code markAsFailed(String)}
 *  for the guards enforcing this.</p>
 */
public enum ReportStatus {
  PENDING,
  GENERATING,
  COMPLETED,
  FAILED
}
