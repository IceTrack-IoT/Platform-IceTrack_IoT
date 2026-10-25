package pe.edu.upc.ice.track.platform.reporting.infrastructure.storage.gcs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Creates the Google Cloud Storage client used by {@link GcsReportFileStorage}.
 *
 * <p>Only active when {@code reporting.storage.provider=gcs}, so a development machine without
 * Google credentials still starts normally with the local adapter.</p>
 *
 * <p>Credentials are resolved in this order:</p>
 * <ol>
 *   <li>{@code reporting.storage.gcs.credentials-path}, when set: a service-account JSON key
 *       file. Meant for development against a real bucket; avoid it in production.</li>
 *   <li>Application Default Credentials otherwise: on Google Cloud (Cloud Run, GKE, Compute
 *       Engine) that is the attached service account, with no key file anywhere in the
 *       project; locally it is {@code gcloud auth application-default login} or the
 *       {@code GOOGLE_APPLICATION_CREDENTIALS} variable.</li>
 * </ol>
 */
@Configuration
@ConditionalOnProperty(name = "reporting.storage.provider", havingValue = "gcs")
public class GcsStorageConfiguration {

  @Bean
  public Storage reportingStorage(
      @Value("${reporting.storage.gcs.project-id:}") String projectId,
      @Value("${reporting.storage.gcs.credentials-path:}") String credentialsPath) {
    var builder = StorageOptions.newBuilder();
    if (!projectId.isBlank()) {
      builder.setProjectId(projectId);
    }
    if (!credentialsPath.isBlank()) {
      try (var stream = new FileInputStream(credentialsPath)) {
        builder.setCredentials(GoogleCredentials.fromStream(stream));
      } catch (IOException ex) {
        throw new UncheckedIOException("Could not read GCS credentials file " + credentialsPath, ex);
      }
    }
    return builder.build().getService();
  }
}
