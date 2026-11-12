package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.device.application.results.EdgeConfiguration;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ActiveEdgeGrant;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.EQUIPMENT_ID;

@ExtendWith(MockitoExtension.class)
class ExternalServiceRequestServiceForDeviceTest {

  @Mock
  private ServiceRequestContextFacade serviceRequestContextFacade;

  @InjectMocks
  private ExternalServiceRequestServiceForDevice service;

  @Test
  @DisplayName("fetchActiveEdgeGrants(Long) -> maps every active grant to an edge configuration grant (AAA)")
  void fetchActiveEdgeGrantsMaps() {
    // Arrange
    var expiresAt = Instant.parse("2026-02-01T00:00:00Z");
    when(serviceRequestContextFacade.fetchActiveEdgeGrants(EQUIPMENT_ID)).thenReturn(List.of(
        new ActiveEdgeGrant("sha-1", 5L, 9L, expiresAt),
        new ActiveEdgeGrant("sha-2", 6L, 10L, expiresAt.plusSeconds(60))));

    // Act
    var result = service.fetchActiveEdgeGrants(EQUIPMENT_ID);

    // Assert
    assertThat(result).containsExactly(
        new EdgeConfiguration.Grant("sha-1", 5L, 9L, expiresAt),
        new EdgeConfiguration.Grant("sha-2", 6L, 10L, expiresAt.plusSeconds(60)));
    verify(serviceRequestContextFacade, times(1)).fetchActiveEdgeGrants(EQUIPMENT_ID);
  }

  @Test
  @DisplayName("fetchActiveEdgeGrants(Long) -> empty list when there is no active grant (AAA)")
  void fetchActiveEdgeGrantsNone() {
    // Arrange
    when(serviceRequestContextFacade.fetchActiveEdgeGrants(EQUIPMENT_ID)).thenReturn(List.of());

    // Act & Assert
    assertThat(service.fetchActiveEdgeGrants(EQUIPMENT_ID)).isEmpty();
  }
}
