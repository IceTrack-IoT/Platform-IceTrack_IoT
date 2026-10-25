package pe.edu.upc.ice.track.platform.reporting.infrastructure.storage.gcs;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.ReportFileStorage;

import java.time.Duration;
import java.util.Objects;
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
 * <p>Any {@code StorageException} from the SDK is left to propagate unchecked:
 * {@code ReportCommandServiceImpl} turns a failed upload into a {@code FAILED} report.</p>
 */
@Service
@ConditionalOnProperty(name = "reporting.storage.provider", havingValue = "gcs")
public class GcsReportFileStorage implements ReportFileStorage {

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
  public String generateDownloadUrl(String key, Duration validFor) {
    Objects.requireNonNull(key, "key must not be null");
    var blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, key)).build();
    return storage.signUrl(
            blobInfo,
            validFor.toSeconds(),
            TimeUnit.SECONDS,
            Storage.SignUrlOption.withV4Signature())
        .toString();
  }

  @Override
  public void delete(String key) {
    Objects.requireNonNull(key, "key must not be null");
    storage.delete(BlobId.of(bucket, key));
  }
}
