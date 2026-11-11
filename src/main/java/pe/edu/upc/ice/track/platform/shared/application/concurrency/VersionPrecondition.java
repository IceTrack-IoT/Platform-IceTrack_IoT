package pe.edu.upc.ice.track.platform.shared.application.concurrency;

import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;

import java.util.Objects;
import java.util.Optional;

/**
 * Checks the version a client says it edited against the version a resource is at now.
 *
 * <p>Clients send the version they read in {@code If-Match}. When it no longer matches, somebody
 * else changed the resource in between, and applying the edit would silently overwrite that change.
 * The edit is refused with {@code CONCURRENT_MODIFICATION_CONFLICT} instead, whose details name the
 * current version so the client can reload and retry.</p>
 *
 * <p>Without an expected version nothing is checked here; the persistence-level optimistic lock
 * still catches writes that overlap in time.</p>
 */
public final class VersionPrecondition {

  private VersionPrecondition() {
  }

  /**
   * Compares the expected version with the current one.
   *
   * @param expectedVersion the version the client edited, or {@code null} when it sent none
   * @param currentVersion  the version the resource is at; {@code null} is read as {@code 0}
   * @return the conflict to report, or empty when the versions match or none was expected
   */
  public static Optional<ApplicationError> check(Long expectedVersion, Long currentVersion) {
    if (expectedVersion == null) {
      return Optional.empty();
    }
    var current = currentVersion == null ? 0L : currentVersion;
    if (Objects.equals(expectedVersion, current)) {
      return Optional.empty();
    }
    return Optional.of(ApplicationError.concurrentModification(
        "The resource was changed meanwhile: If-Match names version %d, but the current version is %d"
            .formatted(expectedVersion, current)));
  }
}
