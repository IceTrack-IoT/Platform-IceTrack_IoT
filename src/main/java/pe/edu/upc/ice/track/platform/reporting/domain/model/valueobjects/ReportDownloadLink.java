package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

import java.time.Instant;
import java.util.Objects;

/**
 * Value object representing a temporary link to download a report's file, with what a client
 * needs to present it before the download starts.
 *
 * <p>It is computed on demand and never persisted: the link stops working at {@code expiresAt}, so
 * a new one is requested each time the user wants the file.</p>
 *
 * @param url         the temporary download URL; asks the browser to save the file under
 *                    {@code fileName} where the storage supports it
 * @param expiresAt   when the URL stops working
 * @param fileName    the suggested file name, extension included
 * @param contentType the MIME type of the file
 * @param sizeBytes   the size of the file in bytes
 */
public record ReportDownloadLink(String url, Instant expiresAt, String fileName, String contentType, long sizeBytes) {

  public ReportDownloadLink {
    Objects.requireNonNull(url, "url must not be null");
    Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    Objects.requireNonNull(fileName, "fileName must not be null");
    Objects.requireNonNull(contentType, "contentType must not be null");
  }
}
