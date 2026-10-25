package pe.edu.upc.ice.track.platform.reporting.infrastructure.storage.local;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.reporting.application.internal.outboundservices.storage.ReportFileStorage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Adapter for {@link ReportFileStorage} that writes to the local disk.
 *
 * <p>It is the <strong>default</strong> ({@code reporting.storage.provider} missing or
 * {@code local}) so the application starts on any machine without Google credentials, and tests
 * need no network. Files go under {@code reporting.storage.local.directory} (default: a folder in
 * the system temp directory).</p>
 *
 * <p>There is no real "temporary URL" on a disk, so {@link #generateDownloadUrl} returns the
 * {@code file:} URI of the stored file: enough to locate and open it while developing, but not
 * something a browser can fetch from another machine. That difference is intentional and the
 * reason this adapter must not be used in production.</p>
 */
@Service
@ConditionalOnProperty(name = "reporting.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalReportFileStorage implements ReportFileStorage {

  private final Path root;

  public LocalReportFileStorage(
      @Value("${reporting.storage.local.directory:${java.io.tmpdir}/icetrack-reports}") String directory) {
    this.root = Path.of(directory).toAbsolutePath().normalize();
  }

  @Override
  public void store(String key, byte[] content, String contentType) {
    Objects.requireNonNull(content, "content must not be null");
    Path target = resolve(key);
    try {
      Files.createDirectories(target.getParent());
      Files.write(target, content);
    } catch (IOException ex) {
      throw new UncheckedIOException("Could not store report file " + key, ex);
    }
  }

  @Override
  public String generateDownloadUrl(String key, Duration validFor) {
    return resolve(key).toUri().toString();
  }

  @Override
  public void delete(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException ex) {
      throw new UncheckedIOException("Could not delete report file " + key, ex);
    }
  }

  /**
   * Resolves a key to a path, refusing any key that would escape the storage root (e.g.
   * {@code ../../etc/passwd}).
   */
  private Path resolve(String key) {
    Objects.requireNonNull(key, "key must not be null");
    Path resolved = root.resolve(key).normalize();
    if (!resolved.startsWith(root)) {
      throw new IllegalArgumentException("Invalid storage key: " + key);
    }
    return resolved;
  }
}
