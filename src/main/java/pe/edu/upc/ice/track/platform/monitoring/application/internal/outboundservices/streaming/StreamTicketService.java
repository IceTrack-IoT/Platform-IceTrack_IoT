package pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.streaming;

import pe.edu.upc.ice.track.platform.monitoring.application.results.IssuedStreamTicket;

import java.util.Optional;

/**
 * Outbound port issuing and redeeming the stream tickets that open the telemetry stream.
 *
 * <p>A ticket is opaque, cryptographically random, bound to one owner profile, valid for
 * {@link IssuedStreamTicket#TTL} and redeemable once.</p>
 */
public interface StreamTicketService {

  /**
   * Issues a ticket bound to an owner profile.
   *
   * @param ownerProfileId identifier of the owner profile the ticket opens a stream for; required
   * @return the issued ticket, or empty when too many tickets are already pending, for this owner or
   *         in total, and the caller must retry later
   */
  Optional<IssuedStreamTicket> issue(Long ownerProfileId);

  /**
   * Redeems a ticket, consuming it whatever the outcome.
   *
   * @param ticket the ticket presented by the client; may be {@code null}
   * @return the owner profile the ticket was issued for, or empty when the ticket is missing,
   *         malformed, unknown, already redeemed or expired
   */
  Optional<Long> redeem(String ticket);
}
