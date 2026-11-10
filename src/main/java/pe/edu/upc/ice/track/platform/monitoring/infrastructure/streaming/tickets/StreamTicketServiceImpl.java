package pe.edu.upc.ice.track.platform.monitoring.infrastructure.streaming.tickets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.streaming.StreamTicketService;
import pe.edu.upc.ice.track.platform.monitoring.application.results.IssuedStreamTicket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * In-memory implementation of {@link StreamTicketService}.
 *
 * <p>A ticket is {@value #PREFIX} followed by 32 bytes from {@link SecureRandom}, hex encoded (256
 * bits of entropy). Only its SHA-256 digest is kept, like refresh tokens and device API keys, so a
 * heap dump never yields a usable ticket. Redeeming removes the entry atomically before its
 * expiration is checked, so a ticket opens at most one stream even when two requests present it at
 * the same instant, and an expired ticket is consumed as well.</p>
 *
 * <p>Pending tickets are bounded, per owner and in total, so an authenticated caller cannot grow the
 * map without limit; a refused issuance is answered {@code 429} by the REST layer. Expired tickets are
 * purged periodically. Tickets live in this instance's memory: with several replicas, the ticket must
 * be redeemed on the instance that issued it (sticky sessions), or the store moved to a shared cache.</p>
 */
@Service
public class StreamTicketServiceImpl implements StreamTicketService {

  static final String PREFIX = "st_";
  static final Duration TICKET_TTL = IssuedStreamTicket.TTL;
  private static final int SECRET_BYTES = 32;
  private static final Pattern TICKET_FORMAT = Pattern.compile(PREFIX + "[0-9a-f]{" + SECRET_BYTES * 2 + "}");
  private static final String DIGEST_ALGORITHM = "SHA-256";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final HexFormat HEX = HexFormat.of();

  private final ConcurrentMap<String, PendingTicket> pendingByDigest = new ConcurrentHashMap<>();
  private final Clock clock;
  private final int maxPendingPerOwner;
  private final int maxPendingTotal;

  /**
   * Creates the store used by the application.
   *
   * @param maxPendingPerOwner unredeemed, unexpired tickets one owner may hold at once
   * @param maxPendingTotal    tickets this instance keeps at most
   */
  @Autowired
  public StreamTicketServiceImpl(
      @Value("${monitoring.telemetry-stream.max-pending-tickets-per-owner:10}") int maxPendingPerOwner,
      @Value("${monitoring.telemetry-stream.max-pending-tickets:10000}") int maxPendingTotal) {
    this(Clock.systemUTC(), maxPendingPerOwner, maxPendingTotal);
  }

  /**
   * Creates a store reading time from the given clock.
   *
   * @param clock              the clock expirations are computed and checked with
   * @param maxPendingPerOwner unredeemed, unexpired tickets one owner may hold at once; at least 1
   * @param maxPendingTotal    tickets this instance keeps at most; at least 1
   */
  StreamTicketServiceImpl(Clock clock, int maxPendingPerOwner, int maxPendingTotal) {
    if (maxPendingPerOwner < 1 || maxPendingTotal < 1) {
      throw new IllegalArgumentException("Stream ticket limits must be at least 1");
    }
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.maxPendingPerOwner = maxPendingPerOwner;
    this.maxPendingTotal = maxPendingTotal;
  }

  // inherited javadoc
  @Override
  public Optional<IssuedStreamTicket> issue(Long ownerProfileId) {
    Objects.requireNonNull(ownerProfileId, "ownerProfileId must not be null");
    var now = clock.instant();
    if (pendingByDigest.size() >= maxPendingTotal) {
      purgeExpired(now);
      if (pendingByDigest.size() >= maxPendingTotal) {
        return Optional.empty();
      }
    }
    // A soft limit: two concurrent issuances may both pass it, which only lets one extra ticket in.
    if (countPending(ownerProfileId, now) >= maxPendingPerOwner) {
      return Optional.empty();
    }
    var secret = new byte[SECRET_BYTES];
    SECURE_RANDOM.nextBytes(secret);
    var ticket = PREFIX + HEX.formatHex(secret);
    pendingByDigest.put(digestOf(ticket), new PendingTicket(ownerProfileId, now.plus(TICKET_TTL)));
    return Optional.of(new IssuedStreamTicket(ticket, TICKET_TTL));
  }

  // inherited javadoc
  @Override
  public Optional<Long> redeem(String ticket) {
    if (ticket == null) {
      return Optional.empty();
    }
    var presented = ticket.strip();
    if (!TICKET_FORMAT.matcher(presented).matches()) {
      return Optional.empty();
    }
    // Removed before the expiration is checked: single use, whatever the outcome.
    var pending = pendingByDigest.remove(digestOf(presented));
    if (pending == null || !pending.expiresAt().isAfter(clock.instant())) {
      return Optional.empty();
    }
    return Optional.of(pending.ownerProfileId());
  }

  /**
   * Drops the tickets that expired without being redeemed.
   */
  @Scheduled(fixedDelayString = "${monitoring.telemetry-stream.ticket-purge-delay-ms:60000}")
  public void purgeExpiredTickets() {
    purgeExpired(clock.instant());
  }

  /** @return the number of tickets currently kept, expired ones included until the next purge */
  int pendingCount() {
    return pendingByDigest.size();
  }

  private void purgeExpired(Instant now) {
    pendingByDigest.values().removeIf(pending -> !pending.expiresAt().isAfter(now));
  }

  private long countPending(Long ownerProfileId, Instant now) {
    return pendingByDigest.values().stream()
        .filter(pending -> pending.ownerProfileId().equals(ownerProfileId) && pending.expiresAt().isAfter(now))
        .count();
  }

  private static String digestOf(String ticket) {
    try {
      return HEX.formatHex(MessageDigest.getInstance(DIGEST_ALGORITHM).digest(ticket.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      // Every Java platform implementation is required to support SHA-256.
      throw new IllegalStateException(DIGEST_ALGORITHM + " is not available", e);
    }
  }

  /**
   * A ticket waiting to be redeemed.
   *
   * @param ownerProfileId the owner profile the ticket opens a stream for
   * @param expiresAt      the instant the ticket stops being redeemable
   */
  private record PendingTicket(Long ownerProfileId, Instant expiresAt) {
  }
}
