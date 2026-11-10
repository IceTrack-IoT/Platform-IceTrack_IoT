package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.application.results.IssuedStreamTicket;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.StreamTicketResource;

/**
 * Assembler converting an {@link IssuedStreamTicket} into its REST resource.
 */
public final class StreamTicketResourceFromResultAssembler {

  private StreamTicketResourceFromResultAssembler() {
  }

  /**
   * @param issued the ticket just issued
   * @return the resource carrying the ticket and its lifetime in seconds
   */
  public static StreamTicketResource toResourceFromResult(IssuedStreamTicket issued) {
    return new StreamTicketResource(issued.ticket(), issued.expiresIn().toSeconds());
  }
}
