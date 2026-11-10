package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

/**
 * Command to redeem a stream ticket when the telemetry stream is opened.
 *
 * <p>Redeeming consumes the ticket whatever the outcome, so a ticket opens at most one stream.</p>
 *
 * @param ticket the ticket presented by the client, as issued ({@code st_...}); may be {@code null}
 */
public record RedeemStreamTicketCommand(String ticket) {
}
