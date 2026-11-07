package pe.edu.upc.ice.track.platform.monitoring.application.commandservices;

import pe.edu.upc.ice.track.platform.monitoring.application.results.IssuedStreamTicket;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.IssueStreamTicketCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RedeemStreamTicketCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.Optional;

/**
 * Application service handling the stream ticket handshake of the telemetry stream.
 *
 * <p>An owner exchanges its JWT for a ticket, then opens the stream with that ticket. The stream is
 * scoped to the owner profile the ticket was issued for, so it only ever carries that owner's
 * units.</p>
 */
public interface TelemetryStreamCommandService {

  /**
   * Issues a stream ticket for the authenticated owner.
   *
   * @param command the command naming the authenticated caller
   * @return the issued ticket; {@code ACCESS_DENIED} when the caller holds no owner profile, or
   *         {@code TOO_MANY_REQUESTS} when too many tickets are pending
   */
  Result<IssuedStreamTicket, ApplicationError> handle(IssueStreamTicketCommand command);

  /**
   * Redeems a stream ticket, consuming it.
   *
   * @param command the command carrying the ticket presented by the client
   * @return the owner profile the stream is opened for, or empty when the ticket is missing,
   *         malformed, unknown, already redeemed or expired
   */
  Optional<Long> handle(RedeemStreamTicketCommand command);
}
