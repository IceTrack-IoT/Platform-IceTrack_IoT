package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeoLocation;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingFailureReason;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingStatus;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.GeocodingFailureReasonResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.GeocodingStatusResource;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;

import static org.assertj.core.api.Assertions.assertThat;

class SiteResourceFromEntityAssemblerTest {

  @Test
  @DisplayName("toResourceFromEntity(Site) -> maps a located site with its location and navigation url (AAA)")
  void toResourceLocatedSite() {
    // Arrange
    var site = AssetsDomainFixtures.siteBuilder().equipmentCount(3).version(5L).build();

    // Act
    var resource = SiteResourceFromEntityAssembler.toResourceFromEntity(site);

    // Assert
    assertThat(resource.id()).isEqualTo(AssetsDomainFixtures.SITE_ID);
    assertThat(resource.ownerId()).isEqualTo(AssetsDomainFixtures.OWNER_ID);
    assertThat(resource.name()).isEqualTo(AssetsDomainFixtures.VALID_SITE_NAME);
    assertThat(resource.address()).isEqualTo(AssetsDomainFixtures.VALID_ADDRESS);
    assertThat(resource.contactName()).isEqualTo(AssetsDomainFixtures.VALID_CONTACT_NAME);
    assertThat(resource.phone()).isEqualTo(AssetsDomainFixtures.VALID_PHONE);
    assertThat(resource.equipmentCount()).isEqualTo(3);
    assertThat(resource.geocodingStatus()).isEqualTo(GeocodingStatusResource.RESOLVED);
    assertThat(resource.geocodingFailureReason()).isNull();
    assertThat(resource.location().latitude()).isEqualTo(AssetsDomainFixtures.LIMA_LATITUDE);
    assertThat(resource.location().longitude()).isEqualTo(AssetsDomainFixtures.LIMA_LONGITUDE);
    assertThat(resource.location().placeId()).isEqualTo(AssetsDomainFixtures.VALID_PLACE_ID);
    assertThat(resource.version()).isEqualTo(5L);
  }

  @Test
  @DisplayName("toResourceFromEntity(Site) -> builds a Google Maps navigation url from the coordinates and place id (AAA)")
  void toResourceNavigationUrl() {
    // Arrange
    var site = AssetsDomainFixtures.site();

    // Act
    var resource = SiteResourceFromEntityAssembler.toResourceFromEntity(site);

    // Assert
    assertThat(resource.navigationUrl()).isEqualTo(
        "https://www.google.com/maps/dir/?api=1&destination=-12.0931,-77.0465&destination_place_id="
            + AssetsDomainFixtures.VALID_PLACE_ID);
  }

  @Test
  @DisplayName("toResourceFromEntity(Site) -> omits the place id from the navigation url when the location has none (AAA)")
  void toResourceNavigationUrlWithoutPlaceId() {
    // Arrange
    var site = AssetsDomainFixtures.siteBuilder()
        .location(new GeoLocation(AssetsDomainFixtures.LIMA_LATITUDE, AssetsDomainFixtures.LIMA_LONGITUDE, null))
        .build();

    // Act
    var resource = SiteResourceFromEntityAssembler.toResourceFromEntity(site);

    // Assert
    assertThat(resource.navigationUrl())
        .isEqualTo("https://www.google.com/maps/dir/?api=1&destination=-12.0931,-77.0465");
  }

  @Test
  @DisplayName("toResourceFromEntity(Site) -> exposes no location nor navigation url while the lookup is pending (AAA)")
  void toResourcePendingSite() {
    // Arrange
    var site = AssetsDomainFixtures.siteBuilder().unlocated(GeocodingStatus.PENDING, null).build();

    // Act
    var resource = SiteResourceFromEntityAssembler.toResourceFromEntity(site);

    // Assert
    assertThat(resource.geocodingStatus()).isEqualTo(GeocodingStatusResource.PENDING);
    assertThat(resource.geocodingFailureReason()).isNull();
    assertThat(resource.location()).isNull();
    assertThat(resource.navigationUrl()).isNull();
  }

  @Test
  @DisplayName("toResourceFromEntity(Site) -> reports the failure reason of a failed lookup (AAA)")
  void toResourceFailedSite() {
    // Arrange
    var site = AssetsDomainFixtures.siteBuilder()
        .unlocated(GeocodingStatus.FAILED, GeocodingFailureReason.PROVIDER_UNAVAILABLE).build();

    // Act
    var resource = SiteResourceFromEntityAssembler.toResourceFromEntity(site);

    // Assert
    assertThat(resource.geocodingStatus()).isEqualTo(GeocodingStatusResource.FAILED);
    assertThat(resource.geocodingFailureReason()).isEqualTo(GeocodingFailureReasonResource.PROVIDER_UNAVAILABLE);
    assertThat(resource.location()).isNull();
  }

  @Test
  @DisplayName("toResourceFromEntity(Site) -> reports UNKNOWN when an unlocated site recorded no reason (AAA)")
  void toResourceNotFoundWithoutReason() {
    // Arrange
    var site = AssetsDomainFixtures.siteBuilder().unlocated(GeocodingStatus.NOT_FOUND, null).build();

    // Act
    var resource = SiteResourceFromEntityAssembler.toResourceFromEntity(site);

    // Assert
    assertThat(resource.geocodingFailureReason()).isEqualTo(GeocodingFailureReasonResource.UNKNOWN);
  }
}
