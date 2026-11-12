package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalProfilesServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.streaming.StreamTicketService;
import pe.edu.upc.ice.track.platform.monitoring.application.results.IssuedStreamTicket;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.IssueStreamTicketCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RedeemStreamTicketCommand;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.OWNER_PROFILE_ID;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.USERNAME;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;

@ExtendWith(MockitoExtension.class)
class TelemetryStreamCommandServiceImplTest {

  private static final String TICKET = "ticket-abc";

  @Mock
  private ExternalProfilesServiceForMonitoring externalProfilesService;
  @Mock
  private StreamTicketService streamTicketService;

  @InjectMocks
  private TelemetryStreamCommandServiceImpl service;

  @Test
  @DisplayName("handle(IssueStreamTicketCommand) -> issues a ticket for the owner profile (AAA)")
  void issueSucceeds() {
    // Arrange
    var issued = new IssuedStreamTicket(TICKET, IssuedStreamTicket.TTL);
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(streamTicketService.issue(OWNER_PROFILE_ID)).thenReturn(Optional.of(issued));

    // Act
    var result = service.handle(new IssueStreamTicketCommand(USERNAME));

    // Assert
    assertThat(successValue(result)).isSameAs(issued);
    verify(streamTicketService, times(1)).issue(OWNER_PROFILE_ID);
  }

  @Test
  @DisplayName("handle(IssueStreamTicketCommand) without owner profile -> ACCESS_DENIED (AAA)")
  void issueDeniedWithoutOwnerProfile() {
    // Arrange
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(new IssueStreamTicketCommand(USERNAME));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(streamTicketService);
  }

  @Test
  @DisplayName("handle(IssueStreamTicketCommand) with too many pending tickets -> TOO_MANY_REQUESTS (AAA)")
  void issueRefusedWhenTooManyPending() {
    // Arrange
    when(externalProfilesService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(streamTicketService.issue(OWNER_PROFILE_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(new IssueStreamTicketCommand(USERNAME));

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("TOO_MANY_REQUESTS");
    assertThat(error.details()).contains(String.valueOf(IssuedStreamTicket.TTL.toSeconds()));
  }

  @Test
  @DisplayName("handle(RedeemStreamTicketCommand) valid ticket -> returns the owner profile id (AAA)")
  void redeemReturnsOwner() {
    // Arrange
    when(streamTicketService.redeem(TICKET)).thenReturn(Optional.of(OWNER_PROFILE_ID));

    // Act
    var owner = service.handle(new RedeemStreamTicketCommand(TICKET));

    // Assert
    assertThat(owner).contains(OWNER_PROFILE_ID);
    verifyNoInteractions(externalProfilesService);
  }

  @Test
  @DisplayName("handle(RedeemStreamTicketCommand) unknown or spent ticket -> empty (AAA)")
  void redeemReturnsEmptyForInvalidTicket() {
    // Arrange
    when(streamTicketService.redeem(TICKET)).thenReturn(Optional.empty());

    // Act
    var owner = service.handle(new RedeemStreamTicketCommand(TICKET));

    // Assert
    assertThat(owner).isEmpty();
  }
}
