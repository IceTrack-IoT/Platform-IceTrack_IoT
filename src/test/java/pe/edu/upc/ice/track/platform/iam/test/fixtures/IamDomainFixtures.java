package pe.edu.upc.ice.track.platform.iam.test.fixtures;

import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.google.GoogleUserInfo;
import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.RefreshToken;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.*;

import java.time.Duration;
import java.time.Instant;

/**
 * Canonical constants and domain object factories for the iam context. Every factory returns a
 * fresh, valid instance; tests MUST NOT build iam domain objects inline.
 */
public final class IamDomainFixtures {

  private IamDomainFixtures() {
    throw new AssertionError("IamDomainFixtures is a utility class and must not be instantiated.");
  }

  // ---------- Constants ----------

  public static final Long USER_ID = 10L;
  public static final Long OTHER_USER_ID = 11L;
  public static final Long ROLE_ID = 1L;
  public static final Long PROFILE_ID = 500L;
  public static final Long RUC = 20123456789L;

  public static final String USERNAME = "jperez";
  public static final String OTHER_USERNAME = "mgarcia";
  public static final String PASSWORD = "Secret123";
  public static final String ENCODED_PASSWORD = "{bcrypt}encoded-secret";
  public static final String EMAIL = "jperez@example.com";
  public static final String FULL_NAME = "Juan Perez";
  public static final String GOOGLE_SUB = "google-sub-123";
  public static final String GOOGLE_ID_TOKEN = "google-id-token";
  public static final String ACCESS_TOKEN = "access.jwt.token";
  public static final String RAW_REFRESH_TOKEN = "raw-refresh-token";
  public static final String OTHER_RAW_REFRESH_TOKEN = "other-raw-refresh-token";
  public static final String REFRESH_DIGEST = "digest-of-raw-refresh-token";
  public static final String OTHER_REFRESH_DIGEST = "digest-of-other-raw-refresh-token";
  public static final String SPECIALITY = "Refrigeration";
  public static final String CERTIFICATION_NUMBER = "CERT-001";

  public static final Duration REFRESH_TTL = Duration.ofDays(7);
  public static final Duration GRACE_PERIOD = Duration.ofSeconds(30);

  // ---------- Roles ----------

  public static Role ownerRole() {
    return new Role(ROLE_ID, Roles.OWNER_ROLE);
  }

  public static Role technicianRole() {
    return new Role(2L, Roles.TECHNICIAN_ROLE);
  }

  // ---------- Value objects ----------

  public static ContactDetails contactDetails() {
    return new ContactDetails("999888777", "Av. Lima", "123", "Lima", "15001", "Peru");
  }

  public static SessionTokens sessionTokens() {
    return new SessionTokens(ACCESS_TOKEN, RAW_REFRESH_TOKEN);
  }

  // ---------- Users ----------

  public static User localOwner() {
    return new User(USER_ID, USERNAME, ENCODED_PASSWORD, ownerRole(), AuthProvider.LOCAL, null);
  }

  public static User localTechnician() {
    return new User(USER_ID, USERNAME, ENCODED_PASSWORD, technicianRole(), AuthProvider.LOCAL, null);
  }

  public static User googleOwner() {
    return new User(USER_ID, USERNAME, "", ownerRole(), AuthProvider.GOOGLE, GOOGLE_SUB);
  }

  public static User googleTechnician() {
    return new User(USER_ID, USERNAME, "", technicianRole(), AuthProvider.GOOGLE, GOOGLE_SUB);
  }

  // ---------- Refresh tokens ----------

  public static RefreshToken activeRefreshToken() {
    return activeRefreshToken(USER_ID, REFRESH_DIGEST, ClientChannel.WEB);
  }

  public static RefreshToken activeRefreshToken(Long userId, String digest, ClientChannel channel) {
    return new RefreshToken(1L, userId, digest, Instant.now().plus(REFRESH_TTL), false, null, null, channel);
  }

  public static RefreshToken expiredRefreshToken() {
    return new RefreshToken(1L, USER_ID, REFRESH_DIGEST, Instant.now().minusSeconds(60), false, null, null,
        ClientChannel.WEB);
  }

  public static RefreshToken revokedRefreshToken() {
    return new RefreshToken(1L, USER_ID, REFRESH_DIGEST, Instant.now().plus(REFRESH_TTL), true,
        Instant.now().minus(Duration.ofHours(1)), null, ClientChannel.WEB);
  }

  public static RefreshToken rotatedRefreshToken(Instant revokedAt, ClientChannel channel) {
    return new RefreshToken(1L, USER_ID, REFRESH_DIGEST, Instant.now().plus(REFRESH_TTL), true, revokedAt,
        OTHER_REFRESH_DIGEST, channel);
  }

  // ---------- Google identity ----------

  public static GoogleUserInfo googleUserInfo() {
    return new GoogleUserInfo(GOOGLE_SUB, EMAIL, true, FULL_NAME, "Juan", "Perez", "https://pic.example/p.png");
  }

  public static GoogleUserInfo unverifiedGoogleUserInfo() {
    return new GoogleUserInfo(GOOGLE_SUB, EMAIL, false, FULL_NAME, "Juan", "Perez", null);
  }

  // ---------- Persistence simulation ----------

  /** Returns a copy of the user carrying the canonical persisted id, as a repository save would. */
  public static User persisted(User user) {
    return new User(USER_ID, user.getUsername(), user.getPassword(), user.getRole(), user.getProvider(),
        user.getExternalId());
  }
}
