package pe.edu.upc.ice.track.platform.reporting.infrastructure.storage.gcs;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.ReportFileStorage;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.SignedDownloadUrl;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for {@link ReportFileStorage} backed by Google Cloud Storage.
 *
 * <p>Activated with {@code reporting.storage.provider=gcs}. Files are stored as objects in a
 * single <strong>private</strong> bucket ({@code reporting.storage.gcs.bucket}); nothing is ever
 * made public. A user reaches a file only through a V4 <em>signed URL</em>: a URL that embeds a
 * signature and an expiry, produced by this application with the service account's private key
 * (or through the IAM {@code signBlob} API when running on Google Cloud without a key file).
 * Whoever holds the URL can download that one object until it expires, and nothing else.</p>
 *
 * <p>Each URL is signed with {@code response-content-disposition=attachment; filename="..."}, so
 * Cloud Storage answers the download with that header and the browser saves the file instead of
 * opening it in place. The expiry reported with the URL is read back from the signature itself
 * ({@code X-Goog-Date} plus {@code X-Goog-Expires}), so it is the instant Cloud Storage will start
 * refusing the URL.</p>
 *
 * <p>Any {@code StorageException} from the SDK is left to propagate unchecked:
 * {@code ReportCommandServiceImpl} turns a failed upload into a {@code FAILED} report.</p>
 */
@Service
@ConditionalOnProperty(name = "reporting.storage.provider", havingValue = "gcs")
public class GcsReportFileStorage implements ReportFileStorage {

  private static final String CONTENT_DISPOSITION_PARAMETER = "response-content-disposition";
  private static final String SIGNED_DATE_PARAMETER = "X-Goog-Date";
  private static final String SIGNED_EXPIRES_PARAMETER = "X-Goog-Expires";
  private static final DateTimeFormatter SIGNED_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

  private final Storage storage;
  private final String bucket;

  public GcsReportFileStorage(Storage storage, @Value("${reporting.storage.gcs.bucket}") String bucket) {
    this.storage = storage;
    this.bucket = bucket;
  }

  @Override
  public void store(String key, byte[] content, String contentType) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(content, "content must not be null");
    var blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, key))
        .setContentType(contentType)
        .build();
    storage.create(blobInfo, content);
  }

  @Override
  public SignedDownloadUrl generateDownloadUrl(String key, Duration validFor, String downloadFileName) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(validFor, "validFor must not be null");
    Objects.requireNonNull(downloadFileName, "downloadFileName must not be null");
    var blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, key)).build();
    // Read before signing, at the second precision of the signature: never later than the date the
    // signer stamps, so the fallback expiry below can only err on the early side.
    var signedNoEarlierThan = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    var url = storage.signUrl(
            blobInfo,
            validFor.toSeconds(),
            TimeUnit.SECONDS,
            Storage.SignUrlOption.withV4Signature(),
            Storage.SignUrlOption.withQueryParams(Map.of(
                CONTENT_DISPOSITION_PARAMETER, "attachment; filename=\"" + downloadFileName + "\"")))
        .toString();
    var expiresAt = expiryEmbeddedIn(url).orElseGet(() -> signedNoEarlierThan.plus(validFor));
    return new SignedDownloadUrl(url, expiresAt);
  }

  @Override
  public void delete(String key) {
    Objects.requireNonNull(key, "key must not be null");
    storage.delete(BlobId.of(bucket, key));
  }

  /**
   * Reads the expiry a V4 signed URL carries: its signing date plus its lifetime in seconds.
   *
   * @param url the signed URL
   * @return the expiry, or empty when either parameter is missing or malformed
   */
  private static Optional<Instant> expiryEmbeddedIn(String url) {
    String signedDate = null;
    String expiresSeconds = null;
    var query = URI.create(url).getRawQuery();
    if (query == null) {
      return Optional.empty();
    }
    for (var pair : query.split("&")) {
      var separator = pair.indexOf('=');
      if (separator <= 0) {
        continue;
      }
      var name = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8);
      var value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
      if (SIGNED_DATE_PARAMETER.equalsIgnoreCase(name)) {
        signedDate = value;
      } else if (SIGNED_EXPIRES_PARAMETER.equalsIgnoreCase(name)) {
        expiresSeconds = value;
      }
    }
    if (signedDate == null || expiresSeconds == null) {
      return Optional.empty();
    }
    try {
      var signedAt = LocalDateTime.parse(signedDate, SIGNED_DATE_FORMAT).toInstant(ZoneOffset.UTC);
      return Optional.of(signedAt.plusSeconds(Long.parseLong(expiresSeconds)));
    } catch (DateTimeParseException | NumberFormatException ex) {
      return Optional.empty();
    }
  }
}
