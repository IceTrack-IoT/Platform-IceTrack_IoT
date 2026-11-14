package pe.edu.upc.ice.track.platform.assets.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalServiceRequestService;
import pe.edu.upc.ice.track.platform.assets.domain.model.queries.GetSitesByOwnerQuery;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsCommandFixtures;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class SiteQueryServiceImplTest {

  @Mock
  private SiteRepository siteRepository;
  @Mock
  private AssetsExternalOwnerService assetsExternalOwnerService;
  @Mock
  private AssetsExternalServiceRequestService assetsExternalServiceRequestService;

  @InjectMocks
  private SiteQueryServiceImpl service;

  @Test
  @DisplayName("handle(GetSitesByOwnerQuery) -> returns the sites of the resolved owner (AAA)")
  void handleSitesByOwnerReturnsOwnerSites() {
    // Arrange
    var sites = List.of(AssetsDomainFixtures.site());
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
    when(siteRepository.findByOwnerId(OWNER_ID)).thenReturn(sites);

    // Act
    var result = service.handle(new GetSitesByOwnerQuery(USERNAME));

    // Assert
    assertThat(result).isEqualTo(sites);
    verify(assetsExternalOwnerService, times(1)).fetchOwnerIdByUsername(USERNAME);
    verify(siteRepository, times(1)).findByOwnerId(OWNER_ID);
    verifyNoMoreInteractions(siteRepository, assetsExternalOwnerService);
  }

  @Test
  @DisplayName("handle(GetSitesByOwnerQuery) -> returns an empty list when the owner has no sites (AAA)")
  void handleSitesByOwnerNoSites() {
    // Arrange
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
    when(siteRepository.findByOwnerId(OWNER_ID)).thenReturn(List.of());

    // Act
    var result = service.handle(new GetSitesByOwnerQuery(USERNAME));

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("handle(GetSitesByOwnerQuery) -> returns an empty list and never reads sites when the caller has no owner profile (AAA)")
  void handleSitesByOwnerNoOwnerProfile() {
    // Arrange
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(new GetSitesByOwnerQuery(USERNAME));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(siteRepository);
  }

  @Test
  @DisplayName("handle(GetSiteByIdQuery) -> returns the site when the owner owns it (AAA)")
  void handleSiteByIdOwnerOwnsSite() {
    // Arrange
    var site = AssetsDomainFixtures.site();
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(site));

    // Act
    var result = service.handle(AssetsCommandFixtures.siteByIdQuery(CallerRole.OWNER));

    // Assert
    assertThat(result).containsSame(site);
    verifyNoInteractions(assetsExternalServiceRequestService);
  }

  @Test
  @DisplayName("handle(GetSiteByIdQuery) -> returns empty when the site belongs to another owner (AAA)")
  void handleSiteByIdForeignSite() {
    // Arrange
    var foreignSite = AssetsDomainFixtures.siteBuilder().ownerId(OTHER_OWNER_ID).build();
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(foreignSite));

    // Act
    var result = service.handle(AssetsCommandFixtures.siteByIdQuery(CallerRole.OWNER));

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("handle(GetSiteByIdQuery) -> returns empty when the site does not exist (AAA)")
  void handleSiteByIdMissingSite() {
    // Arrange
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(AssetsCommandFixtures.siteByIdQuery(CallerRole.OWNER));

    // Assert
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("handle(GetSiteByIdQuery) -> returns empty and never reads the site when the owner has no profile (AAA)")
  void handleSiteByIdNoOwnerProfile() {
    // Arrange
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(AssetsCommandFixtures.siteByIdQuery(CallerRole.OWNER));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(siteRepository);
  }

  @Test
  @DisplayName("handle(GetSiteByIdQuery) -> returns the site to a technician with an active assignment there (AAA)")
  void handleSiteByIdTechnicianAssigned() {
    // Arrange
    var site = AssetsDomainFixtures.site();
    when(assetsExternalServiceRequestService.isTechnicianAssignedToSite(TECHNICIAN_USERNAME, SITE_ID)).thenReturn(true);
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(site));

    // Act
    var result = service.handle(AssetsCommandFixtures.siteByIdQuery(CallerRole.TECHNICIAN));

    // Assert
    assertThat(result).containsSame(site);
    verifyNoInteractions(assetsExternalOwnerService);
  }

  @Test
  @DisplayName("handle(GetSiteByIdQuery) -> returns empty and never reads the site for an unassigned technician (AAA)")
  void handleSiteByIdTechnicianNotAssigned() {
    // Arrange
    when(assetsExternalServiceRequestService.isTechnicianAssignedToSite(TECHNICIAN_USERNAME, SITE_ID)).thenReturn(false);

    // Act
    var result = service.handle(AssetsCommandFixtures.siteByIdQuery(CallerRole.TECHNICIAN));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(siteRepository, assetsExternalOwnerService);
  }
}
