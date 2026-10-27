package pe.edu.upc.ice.track.platform.notifications.infrastructure.push.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Creates the Firebase Admin client used by {@link FcmPushGateway}.
 *
 * <p>Only active when {@code notifications.push.provider=fcm}, so a machine without Firebase
 * credentials starts normally with the no-op gateway.</p>
 *
 * <p>Credentials: {@code notifications.push.fcm.credentials-path} (a service-account JSON key)
 * when set; otherwise Application Default Credentials, which on Google Cloud is the attached
 * service account and needs no key file.</p>
 */
@Configuration
@ConditionalOnProperty(name = "notifications.push.provider", havingValue = "fcm")
public class FcmConfiguration {

  private static final String APP_NAME = "icetrack-notifications";

  @Bean
  public FirebaseMessaging firebaseMessaging(
      @Value("${notifications.push.fcm.project-id:}") String projectId,
      @Value("${notifications.push.fcm.credentials-path:}") String credentialsPath) {
    var app = FirebaseApp.getApps().stream()
        .filter(existing -> APP_NAME.equals(existing.getName()))
        .findFirst()
        .orElseGet(() -> FirebaseApp.initializeApp(buildOptions(projectId, credentialsPath), APP_NAME));
    return FirebaseMessaging.getInstance(app);
  }

  private FirebaseOptions buildOptions(String projectId, String credentialsPath) {
    try {
      GoogleCredentials credentials;
      if (credentialsPath.isBlank()) {
        credentials = GoogleCredentials.getApplicationDefault();
      } else {
        try (var stream = new FileInputStream(credentialsPath)) {
          credentials = GoogleCredentials.fromStream(stream);
        }
      }
      var builder = FirebaseOptions.builder().setCredentials(credentials);
      if (!projectId.isBlank()) {
        builder.setProjectId(projectId);
      }
      return builder.build();
    } catch (IOException ex) {
      throw new UncheckedIOException("Could not load Firebase credentials", ex);
    }
  }
}
