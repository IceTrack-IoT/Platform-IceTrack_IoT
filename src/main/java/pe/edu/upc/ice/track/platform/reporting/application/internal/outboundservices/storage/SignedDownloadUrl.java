package pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage;

import java.time.Instant;
import java.util.Objects;

/**
 * A temporary download URL issued by a {@link ReportFileStorage}, with the instant it stops
 * working.
 *
 * @param url       the URL
 * @param expiresAt when the URL stops working, as embedded in its signature where the storage signs
 *                  it
 */
public record SignedDownloadUrl(String url, Instant expiresAt) {

  public SignedDownloadUrl {
    Objects.requireNonNull(url, "url must not be null");
    Objects.requireNonNull(expiresAt, "expiresAt must not be null");
  }
}
