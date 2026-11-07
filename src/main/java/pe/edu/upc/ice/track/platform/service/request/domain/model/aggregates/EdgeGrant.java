package pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.EdgeGrantIssuedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.EdgeGrantRevokedEvent;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

/**
 * EdgeGrant aggregate root of the {@code service.request} bounded context.
 *
 * <p>A short-lived credential that lets the technician of an accepted service request operate the
 * on-site edge gateway of the unit from the mobile application, without the gateway ever validating
 * a user JWT or holding a signing key. The gateway learns the active grants of its unit from its
 * configuration poll and compares the token the mobile application presents against them.</p>
 *
 * <p>Only the hex SHA-256 digest of the token is held here; the plaintext is handed to the
 * technician once, when the grant is issued. A grant ends either by expiring or by being
 * {@link #revoke revoked} - when the request is reassigned, canceled or completed, or when the
 * technician regenerates it - and a revoked grant is kept, not deleted, so the history of who could
 * operate a gateway and when is preserved.</p>
 *
 * <p>Issuing and revoking register domain events, which the repository adapter publishes once the
 * row is written: the unit's edge configuration changes with every grant, so its version must
 * move.</p>
 */
@Getter
public class EdgeGrant extends AbstractDomainAggregateRoot<EdgeGrant> {

  /** Prefix of every plaintext grant token. */
  public static final String TOKEN_PREFIX = "it_grt_";

  /** Longest accepted revocation reason, matching the column width. */
  public static final int MAX_REVOCATION_REASON_LENGTH = 30;

  private static final Pattern HEX_SHA256 = Pattern.compile("[0-9a-f]{64}");

  private final Long id;
  private final Long serviceRequestId;
  private final Long technicianProfileId;
  private final Long equipmentId;
  private final String tokenDigest;
  private final Instant issuedAt;
  private final Instant expiresAt;
  private Instant revokedAt;
  private String revocationReason;

  /**
   * Reconstitutes a grant, typically from persistence. New grants are created through
   * {@link #issue}.
   *
   * @param id                  the persistence identity, or {@code null} when not yet persisted
   * @param serviceRequestId    the request the grant was issued for; required
   * @param technicianProfileId the technician it was issued to; required
   * @param equipmentId         the unit whose gateway it opens; required
   * @param tokenDigest         hex SHA-256 digest of the token; required
   * @param issuedAt            when it was issued; required
   * @param expiresAt           when it stops being valid; required, after {@code issuedAt}
   * @param revokedAt           when it was revoked, or {@code null} while not revoked
   * @param revocationReason    why it was revoked, or {@code null} while not revoked
   * @throws IllegalArgumentException when a value is missing or invalid
   */
  public EdgeGrant(Long id, Long serviceRequestId, Long technicianProfileId, Long equipmentId, String tokenDigest,
                   Instant issuedAt, Instant expiresAt, Instant revokedAt, String revocationReason) {
    this.id = id;
    this.serviceRequestId = requireValue(serviceRequestId, "serviceRequestId");
    this.technicianProfileId = requireValue(technicianProfileId, "technicianProfileId");
    this.equipmentId = requireValue(equipmentId, "equipmentId");
    if (tokenDigest == null || !HEX_SHA256.matcher(tokenDigest).matches()) {
      throw new IllegalArgumentException("Edge grant tokenDigest must be a hex SHA-256 digest");
    }
    this.tokenDigest = tokenDigest;
    this.issuedAt = requireValue(issuedAt, "issuedAt");
    this.expiresAt = requireValue(expiresAt, "expiresAt");
    if (!expiresAt.isAfter(issuedAt)) {
      throw new IllegalArgumentException("Edge grant expiresAt must be after issuedAt");
    }
    this.revokedAt = revokedAt;
    this.revocationReason = revocationReason;
  }

  /**
   * Issues a new grant for the technician of an accepted request.
   *
   * @param serviceRequestId    the request; required
   * @param technicianProfileId the assigned technician; required
   * @param equipmentId         the unit of the request; required
   * @param tokenDigest         hex SHA-256 digest of the freshly generated token; required
   * @param issuedAt            the issuance instant; required
   * @param expiresAt           the expiry instant; required, after {@code issuedAt}
   * @return the new grant, not yet persisted
   * @throws IllegalArgumentException when a value is missing or invalid
   */
  public static EdgeGrant issue(Long serviceRequestId, Long technicianProfileId, Long equipmentId,
                                String tokenDigest, Instant issuedAt, Instant expiresAt) {
    var grant = new EdgeGrant(null, serviceRequestId, technicianProfileId, equipmentId, tokenDigest,
        issuedAt.truncatedTo(ChronoUnit.MICROS), expiresAt.truncatedTo(ChronoUnit.MICROS), null, null);
    grant.registerDomainEvent(new EdgeGrantIssuedEvent(serviceRequestId, technicianProfileId, equipmentId));
    return grant;
  }

  /**
   * Revokes this grant. Revoking a grant already revoked changes nothing and registers no event.
   *
   * @param reason why the grant ends, such as {@code REASSIGNED}; required
   * @param now    the revocation instant; required
   * @throws IllegalArgumentException when the reason is missing or too long
   */
  public void revoke(String reason, Instant now) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("An edge grant revocation reason is required");
    }
    if (reason.length() > MAX_REVOCATION_REASON_LENGTH) {
      throw new IllegalArgumentException(
          "The edge grant revocation reason must not exceed %d characters".formatted(MAX_REVOCATION_REASON_LENGTH));
    }
    if (isRevoked()) {
      return;
    }
    this.revokedAt = requireValue(now, "now").truncatedTo(ChronoUnit.MICROS);
    this.revocationReason = reason;
    registerDomainEvent(new EdgeGrantRevokedEvent(serviceRequestId, technicianProfileId, equipmentId, reason));
  }

  /** @return {@code true} once the grant has been revoked */
  public boolean isRevoked() {
    return revokedAt != null;
  }

  /**
   * @param at the instant to test
   * @return {@code true} when the grant is neither revoked nor expired at that instant
   */
  public boolean isActiveAt(Instant at) {
    return !isRevoked() && at.isBefore(expiresAt);
  }

  private static <T> T requireValue(T value, String field) {
    if (value == null) {
      throw new IllegalArgumentException("Edge grant %s must not be null".formatted(field));
    }
    return value;
  }
}
