package pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage;

import java.time.Duration;

/**
 * Outbound port through which {@code reporting} stores and retrieves the rendered files of its
 * reports (PDF, Excel, CSV).
 *
 * <p>The application layer depends on this port and on nothing else: it never sees the Google
 * Cloud Storage SDK. Two adapters implement it in {@code reporting.infrastructure.storage}:
 * {@code GcsReportFileStorage} (production) and {@code LocalReportFileStorage} (development and
 * tests), selected with the {@code reporting.storage.provider} property.</p>
 *
 * <p>Files are addressed by an opaque {@code key} (the object name). The port never exposes
 * bytes for download: callers ask for a temporary URL and the client fetches the file straight
 * from the storage, so large files never pass through the application server.</p>
 */
public interface ReportFileStorage {

  /**
   * Stores a file under the given key, overwriting any existing one.
   *
   * @param key         the object name to store it under; required
   * @param content     the file bytes; required
   * @param contentType the MIME type of the file, e.g. {@code application/pdf}; required
   */
  void store(String key, byte[] content, String contentType);

  /**
   * Produces a temporary URL from which the file can be downloaded without credentials.
   *
   * @param key      the object name of the file; required
   * @param validFor how long the URL must keep working; required
   * @return the temporary download URL
   */
  String generateDownloadUrl(String key, Duration validFor);

  /**
   * Deletes a file. Deleting a key that does not exist is not an error.
   *
   * @param key the object name of the file; required
   */
  void delete(String key);
}
