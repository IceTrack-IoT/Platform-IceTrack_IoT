package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

import java.util.Objects;

/**
 * Value object describing the rendered file that backs a completed report.
 *
 * <p>It stores a <em>reference</em> to the file, never its bytes and never a download URL: the
 * bytes live in the file storage (Google Cloud Storage in production) and download URLs are
 * signed on demand because they expire. The {@code key} is opaque to the domain -- only the
 * {@code ReportFileStorage} adapter knows how to resolve it.</p>
 *
 * @param key         the storage key (object name) of the file, e.g. {@code reports/12/5f3c....pdf}
 * @param contentType the MIME type of the file, e.g. {@code application/pdf}
 * @param sizeBytes   the size of the file in bytes
 */
public record ReportFile(String key, String contentType, long sizeBytes) {

  public ReportFile {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(contentType, "contentType must not be null");
    if (key.isBlank()) {
      throw new IllegalArgumentException("key must not be blank");
    }
    if (sizeBytes < 0) {
      throw new IllegalArgumentException("sizeBytes must not be negative");
    }
  }
}
