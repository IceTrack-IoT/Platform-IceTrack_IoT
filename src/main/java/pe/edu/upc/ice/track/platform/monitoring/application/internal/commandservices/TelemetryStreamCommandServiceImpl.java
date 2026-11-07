package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.TelemetryStreamCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.profiles.ExternalProfilesServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.streaming.StreamTicketService;
import pe.edu.upc.ice.track.platform.monitoring.application.results.IssuedStreamTicket;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.IssueStreamTicketCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RedeemStreamTicketCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.Optional;

/**
 * Default implementation of {@link TelemetryStreamCommandService}.
 *
 * <p>The owner profile is resolved from the authenticated username when the ticket is issued, and
 * stored with the ticket. Redeeming therefore touches no database: the stream request then holds no
 * connection for its whole lifetime, which can last 30 minutes.</p>
 */
@Service
@Slf4j
public class TelemetryStreamCommandServiceImpl implements TelemetryStreamCommandService {

  private final ExternalProfilesServiceForMonitoring externalProfilesService;
  private final StreamTicketService streamTicketService;

  public TelemetryStreamCommandServiceImpl(
      ExternalProfilesServiceForMonitoring externalProfilesService,
      StreamTicketService streamTicketService) {
    this.externalProfilesService = externalProfilesService;
    this.streamTicketService = streamTicketService;
  }

  // inherited javadoc
  @Override
  public Result<IssuedStreamTicket, ApplicationError> handle(IssueStreamTicketCommand command) {
    var ownerProfileId = externalProfilesService.fetchOwnerIdByUsername(command.username());
    if (ownerProfileId.isEmpty()) {
      return Result.failure(ApplicationError.accessDenied("The authenticated account has no owner profile"));
    }
    var issued = streamTicketService.issue(ownerProfileId.get());
    if (issued.isEmpty()) {
      log.warn("Refused a stream ticket to owner {}: too many tickets pending", ownerProfileId.get());
      return Result.failure(ApplicationError.tooManyRequests(
          "Too many stream tickets are pending; retry in %d seconds"
              .formatted(IssuedStreamTicket.TTL.toSeconds())));
    }
    return Result.success(issued.get());
  }

  // inherited javadoc
  @Override
  public Optional<Long> handle(RedeemStreamTicketCommand command) {
    return streamTicketService.redeem(command.ticket());
  }
}
