package pe.edu.upc.ice.track.platform.assets.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation.AddressValidationTokenPort;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation.ValidationRateLimiterPort;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.geocoding.GeocodingPort;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.SiteGeocodingPendingEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsCommandFixtures;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;
import pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class SiteCommandServiceImplTest {

  @Mock
  private SiteRepository siteRepository;
  @Mock
  private AssetsExternalOwnerService assetsExternalOwnerService;
  @Mock
  private AddressValidationTokenPort addressValidationTokenPort;
  @Mock
  private GeocodingPort geocodingPort;
  @Mock
  private ValidationRateLimiterPort validationRateLimiter;

  @InjectMocks
  private SiteCommandServiceImpl service;

  // ---------- Helpers ----------

  private void stubOwner() {
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
  }

  private void stubNoOwnerProfile() {
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());
  }

  private void stubValidToken() {
    when(addressValidationTokenPort.verify(VALIDATION_TOKEN, OWNER_ID))
        .thenReturn(Optional.of(AssetsDomainFixtures.verifiedAddress()));
  }

  private void stubInvalidToken() {
    when(addressValidationTokenPort.verify(VALIDATION_TOKEN, OWNER_ID)).thenReturn(Optional.empty());
  }

  private void stubReachableSite(Site site) {
    stubOwner();
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(site));
  }

  private static Site pendingSite() {
    return AssetsDomainFixtures.siteBuilder().unlocated(GeocodingStatus.PENDING, null).build();
  }

  private static GeocodingResult resolvedAt(double latitude, double longitude) {
    return GeocodingResult.resolved(
        new GeoLocation(latitude, longitude, AssetsDomainFixtures.VALID_PLACE_ID),
        AssetsDomainFixtures.VALID_ADDRESS,
        AssetsDomainFixtures.VALID_DISTRICT);
  }

  // ====================================================================================
  // handle(RegisterSiteCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(RegisterSiteCommand) -> registers a RESOLVED site at the verified point when no pin is given (AAA)")
  void handleRegisterSuccessWithoutPin() {
    // Arrange
    var captor = ArgumentCaptor.forClass(Site.class);
    stubOwner();
    stubValidToken();
    when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    var result = service.handle(AssetsCommandFixtures.registerSiteCommand());

    // Assert
    var site = ResultTestUtils.successValue(result);
    assertThat(site.getOwnerId()).isEqualTo(OWNER_ID);
    assertThat(site.getName()).isEqualTo(AssetsDomainFixtures.VALID_SITE_NAME);
    assertThat(site.getAddress().value()).isEqualTo(AssetsDomainFixtures.VALID_ADDRESS);
    assertThat(site.getContactName()).isEqualTo(AssetsDomainFixtures.VALID_CONTACT_NAME);
    assertThat(site.getPhone().value()).isEqualTo(AssetsDomainFixtures.VALID_PHONE);
    assertThat(site.getGeocodingStatus()).isEqualTo(GeocodingStatus.RESOLVED);
    assertThat(site.getLocation()).isEqualTo(AssetsDomainFixtures.geoLocation());
    assertThat(site.getEquipmentCount()).isZero();
    verify(siteRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getSiteId()).isNull();
  }

  @Test
  @DisplayName("handle(RegisterSiteCommand) -> uses the owner pin when it lies within range of the verified point (AAA)")
  void handleRegisterSuccessWithNearbyPin() {
    // Arrange
    stubOwner();
    stubValidToken();
    when(siteRepository.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    var result = service.handle(AssetsCommandFixtures.registerSiteCommand(
        AssetsDomainFixtures.NEARBY_PIN_LATITUDE, AssetsDomainFixtures.NEARBY_PIN_LONGITUDE));

    // Assert
    var location = ResultTestUtils.successValue(result).getLocation();
    assertThat(location.latitude()).isEqualTo(AssetsDomainFixtures.NEARBY_PIN_LATITUDE);
    assertThat(location.longitude()).isEqualTo(AssetsDomainFixtures.NEARBY_PIN_LONGITUDE);
    assertThat(location.placeId()).isEqualTo(AssetsDomainFixtures.VALID_PLACE_ID);
  }

  @Test
  @DisplayName("handle(RegisterSiteCommand) -> PIN_OUT_OF_RANGE when the pin is farther than 150 meters (AAA)")
  void handleRegisterPinOutOfRange() {
    // Arrange
    stubOwner();
    stubValidToken();

    // Act
    var result = service.handle(AssetsCommandFixtures.registerSiteCommand(
        AssetsDomainFixtures.FAR_PIN_LATITUDE, AssetsDomainFixtures.FAR_PIN_LONGITUDE));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("PIN_OUT_OF_RANGE");
    assertThat(error.details()).contains("150 meters");
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(RegisterSiteCommand) -> VALIDATION_ERROR when only one pin coordinate is given (AAA)")
  void handleRegisterIncompletePin() {
    // Arrange
    stubOwner();
    stubValidToken();

    // Act
    var result = service.handle(AssetsCommandFixtures.registerSiteCommand(AssetsDomainFixtures.NEARBY_PIN_LATITUDE, null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).contains("together");
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(RegisterSiteCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleRegisterNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(AssetsCommandFixtures.registerSiteCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(addressValidationTokenPort, siteRepository);
  }

  @Test
  @DisplayName("handle(RegisterSiteCommand) -> ADDRESS_VALIDATION_REQUIRED when the token is missing, forged or expired (AAA)")
  void handleRegisterInvalidToken() {
    // Arrange
    stubOwner();
    stubInvalidToken();

    // Act
    var result = service.handle(AssetsCommandFixtures.registerSiteCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ADDRESS_VALIDATION_REQUIRED");
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(RegisterSiteCommand) -> VALIDATION_ERROR when the site name is blank (AAA)")
  void handleRegisterBlankName() {
    // Arrange
    stubOwner();
    stubValidToken();

    // Act
    var result = service.handle(AssetsCommandFixtures.registerSiteCommand(
        " ", AssetsDomainFixtures.VALID_PHONE, null, null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).contains("name");
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(RegisterSiteCommand) -> VALIDATION_ERROR when the phone is malformed (AAA)")
  void handleRegisterMalformedPhone() {
    // Arrange
    stubOwner();
    stubValidToken();

    // Act
    var result = service.handle(AssetsCommandFixtures.registerSiteCommand(
        AssetsDomainFixtures.VALID_SITE_NAME, "not-a-phone", null, null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).contains("phone");
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(RegisterSiteCommand) -> wraps a save failure in UNEXPECTED_ERROR (AAA)")
  void handleRegisterUnexpectedFailure() {
    // Arrange
    stubOwner();
    stubValidToken();
    when(siteRepository.save(any(Site.class))).thenThrow(new RuntimeException("db down"));

    // Act
    var result = service.handle(AssetsCommandFixtures.registerSiteCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(error.message()).contains("Site registration");
    assertThat(error.details()).isEqualTo("db down");
  }

  // ====================================================================================
  // handle(UpdateSiteInfoCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(UpdateSiteInfoCommand) -> replaces name, contact name and phone of a reachable site (AAA)")
  void handleUpdateInfoSuccess() {
    // Arrange
    var site = AssetsDomainFixtures.site();
    stubReachableSite(site);
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteInfoCommand(null));

    // Assert
    var updated = ResultTestUtils.successValue(result);
    assertThat(updated.getName()).isEqualTo(AssetsCommandFixtures.UPDATED_SITE_NAME);
    assertThat(updated.getContactName()).isEqualTo(AssetsCommandFixtures.UPDATED_CONTACT_NAME);
    assertThat(updated.getPhone().value()).isEqualTo(AssetsCommandFixtures.UPDATED_PHONE);
    verify(siteRepository, times(1)).save(site);
  }

  @Test
  @DisplayName("handle(UpdateSiteInfoCommand) -> succeeds when If-Match names the current version (AAA)")
  void handleUpdateInfoMatchingVersion() {
    // Arrange
    var site = AssetsDomainFixtures.siteBuilder().version(4L).build();
    stubReachableSite(site);
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteInfoCommand(4L));

    // Assert
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  @DisplayName("handle(UpdateSiteInfoCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleUpdateInfoNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteInfoCommand(null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(siteRepository);
  }

  @Test
  @DisplayName("handle(UpdateSiteInfoCommand) -> SITE_NOT_FOUND when the site does not exist (AAA)")
  void handleUpdateInfoMissingSite() {
    // Arrange
    stubOwner();
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteInfoCommand(null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("SITE_NOT_FOUND");
  }

  @Test
  @DisplayName("handle(UpdateSiteInfoCommand) -> SITE_NOT_FOUND when the site belongs to another owner (AAA)")
  void handleUpdateInfoSiteOfAnotherOwner() {
    // Arrange
    stubReachableSite(AssetsDomainFixtures.siteBuilder().ownerId(OTHER_OWNER_ID).build());

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteInfoCommand(null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("SITE_NOT_FOUND");
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(UpdateSiteInfoCommand) -> CONCURRENT_MODIFICATION_CONFLICT when If-Match is stale (AAA)")
  void handleUpdateInfoStaleVersion() {
    // Arrange
    stubReachableSite(AssetsDomainFixtures.siteBuilder().version(4L).build());

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteInfoCommand(3L));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("CONCURRENT_MODIFICATION_CONFLICT");
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(UpdateSiteInfoCommand) -> VALIDATION_ERROR and site untouched when the name is blank (AAA)")
  void handleUpdateInfoBlankName() {
    // Arrange
    var site = AssetsDomainFixtures.site();
    stubReachableSite(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteInfoCommand(" ", AssetsCommandFixtures.UPDATED_PHONE, null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("VALIDATION_ERROR");
    assertThat(site.getName()).isEqualTo(AssetsDomainFixtures.VALID_SITE_NAME);
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(UpdateSiteInfoCommand) -> VALIDATION_ERROR when the phone is malformed (AAA)")
  void handleUpdateInfoMalformedPhone() {
    // Arrange
    var site = AssetsDomainFixtures.site();
    stubReachableSite(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteInfoCommand(AssetsCommandFixtures.UPDATED_SITE_NAME, "12", null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("VALIDATION_ERROR");
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(UpdateSiteInfoCommand) -> rethrows OptimisticLockingFailureException (AAA)")
  void handleUpdateInfoRethrowsOptimisticLock() {
    // Arrange
    var site = AssetsDomainFixtures.site();
    stubReachableSite(site);
    when(siteRepository.save(site)).thenThrow(new OptimisticLockingFailureException("stale"));

    // Act + Assert
    assertThrows(OptimisticLockingFailureException.class,
        () -> service.handle(AssetsCommandFixtures.updateSiteInfoCommand(null)));
  }

  @Test
  @DisplayName("handle(UpdateSiteInfoCommand) -> wraps any other save failure in UNEXPECTED_ERROR (AAA)")
  void handleUpdateInfoUnexpectedFailure() {
    // Arrange
    var site = AssetsDomainFixtures.site();
    stubReachableSite(site);
    when(siteRepository.save(site)).thenThrow(new RuntimeException("db down"));

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteInfoCommand(null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(error.message()).contains("Site update");
  }

  // ====================================================================================
  // handle(UpdateSiteAddressCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(UpdateSiteAddressCommand) -> moves the site to the verified address and keeps it RESOLVED (AAA)")
  void handleUpdateAddressSuccess() {
    // Arrange
    var site = AssetsDomainFixtures.siteBuilder()
        .address("Calle Antigua 1")
        .unlocated(GeocodingStatus.NOT_FOUND, GeocodingFailureReason.ADDRESS_NOT_FOUND)
        .build();
    stubReachableSite(site);
    stubValidToken();
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteAddressCommand(null, null, null));

    // Assert
    var updated = ResultTestUtils.successValue(result);
    assertThat(updated.getAddress().value()).isEqualTo(AssetsDomainFixtures.VALID_ADDRESS);
    assertThat(updated.getGeocodingStatus()).isEqualTo(GeocodingStatus.RESOLVED);
    assertThat(updated.getGeocodingFailureReason()).isNull();
    assertThat(updated.getLocation()).isEqualTo(AssetsDomainFixtures.geoLocation());
  }

  @Test
  @DisplayName("handle(UpdateSiteAddressCommand) -> checks the token against the owner of the site (AAA)")
  void handleUpdateAddressVerifiesTokenForSiteOwner() {
    // Arrange
    var site = AssetsDomainFixtures.site();
    stubReachableSite(site);
    stubValidToken();
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    service.handle(AssetsCommandFixtures.updateSiteAddressCommand(null, null, null));

    // Assert
    verify(addressValidationTokenPort, times(1)).verify(VALIDATION_TOKEN, OWNER_ID);
  }

  @Test
  @DisplayName("handle(UpdateSiteAddressCommand) -> CONCURRENT_MODIFICATION_CONFLICT before checking the token (AAA)")
  void handleUpdateAddressStaleVersion() {
    // Arrange
    stubReachableSite(AssetsDomainFixtures.siteBuilder().version(4L).build());

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteAddressCommand(null, null, 3L));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("CONCURRENT_MODIFICATION_CONFLICT");
    verifyNoInteractions(addressValidationTokenPort);
  }

  @Test
  @DisplayName("handle(UpdateSiteAddressCommand) -> ADDRESS_VALIDATION_REQUIRED when the token is not valid (AAA)")
  void handleUpdateAddressInvalidToken() {
    // Arrange
    stubReachableSite(AssetsDomainFixtures.site());
    stubInvalidToken();

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteAddressCommand(null, null, null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ADDRESS_VALIDATION_REQUIRED");
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(UpdateSiteAddressCommand) -> PIN_OUT_OF_RANGE when the pin is farther than 150 meters (AAA)")
  void handleUpdateAddressPinOutOfRange() {
    // Arrange
    stubReachableSite(AssetsDomainFixtures.site());
    stubValidToken();

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteAddressCommand(
        AssetsDomainFixtures.FAR_PIN_LATITUDE, AssetsDomainFixtures.FAR_PIN_LONGITUDE, null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("PIN_OUT_OF_RANGE");
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(UpdateSiteAddressCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleUpdateAddressNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteAddressCommand(null, null, null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ACCESS_DENIED");
  }

  @Test
  @DisplayName("handle(UpdateSiteAddressCommand) -> SITE_NOT_FOUND when the site belongs to another owner (AAA)")
  void handleUpdateAddressSiteOfAnotherOwner() {
    // Arrange
    stubReachableSite(AssetsDomainFixtures.siteBuilder().ownerId(OTHER_OWNER_ID).build());

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteAddressCommand(null, null, null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("SITE_NOT_FOUND");
    verifyNoInteractions(addressValidationTokenPort);
  }

  @Test
  @DisplayName("handle(UpdateSiteAddressCommand) -> VALIDATION_ERROR when the verified address is blank (AAA)")
  void handleUpdateAddressBlankVerifiedAddress() {
    // Arrange
    var blankAddress = new VerifiedAddress(
        "  ", AssetsDomainFixtures.VALID_PLACE_ID, AssetsDomainFixtures.geoLocation(),
        AssetsDomainFixtures.VALID_DISTRICT, AssetsDomainFixtures.verifiedAddress().area());
    stubReachableSite(AssetsDomainFixtures.site());
    when(addressValidationTokenPort.verify(VALIDATION_TOKEN, OWNER_ID)).thenReturn(Optional.of(blankAddress));

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteAddressCommand(null, null, null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("VALIDATION_ERROR");
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(UpdateSiteAddressCommand) -> rethrows OptimisticLockingFailureException (AAA)")
  void handleUpdateAddressRethrowsOptimisticLock() {
    // Arrange
    var site = AssetsDomainFixtures.site();
    stubReachableSite(site);
    stubValidToken();
    when(siteRepository.save(site)).thenThrow(new OptimisticLockingFailureException("stale"));

    // Act + Assert
    assertThrows(OptimisticLockingFailureException.class,
        () -> service.handle(AssetsCommandFixtures.updateSiteAddressCommand(null, null, null)));
  }

  @Test
  @DisplayName("handle(UpdateSiteAddressCommand) -> wraps any other save failure in UNEXPECTED_ERROR (AAA)")
  void handleUpdateAddressUnexpectedFailure() {
    // Arrange
    var site = AssetsDomainFixtures.site();
    stubReachableSite(site);
    stubValidToken();
    when(siteRepository.save(site)).thenThrow(new RuntimeException("db down"));

    // Act
    var result = service.handle(AssetsCommandFixtures.updateSiteAddressCommand(null, null, null));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("UNEXPECTED_ERROR");
    assertThat(error.message()).contains("Site address update");
  }

  // ====================================================================================
  // handle(RetrySiteGeocodingCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(RetrySiteGeocodingCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleRetryNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(AssetsCommandFixtures.retrySiteGeocodingCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(validationRateLimiter, geocodingPort);
  }

  @Test
  @DisplayName("handle(RetrySiteGeocodingCommand) -> SITE_NOT_FOUND when the site belongs to another owner (AAA)")
  void handleRetrySiteOfAnotherOwner() {
    // Arrange
    stubReachableSite(AssetsDomainFixtures.siteBuilder().ownerId(OTHER_OWNER_ID).build());

    // Act
    var result = service.handle(AssetsCommandFixtures.retrySiteGeocodingCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("SITE_NOT_FOUND");
    verifyNoInteractions(validationRateLimiter, geocodingPort);
  }

  @Test
  @DisplayName("handle(RetrySiteGeocodingCommand) -> INVALID_STATUS_TRANSITION when the site is already RESOLVED (AAA)")
  void handleRetryAlreadyResolved() {
    // Arrange
    stubReachableSite(AssetsDomainFixtures.site());

    // Act
    var result = service.handle(AssetsCommandFixtures.retrySiteGeocodingCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("INVALID_STATUS_TRANSITION");
    verifyNoInteractions(validationRateLimiter, geocodingPort);
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(RetrySiteGeocodingCommand) -> TOO_MANY_REQUESTS without querying the provider when over quota (AAA)")
  void handleRetryRateLimited() {
    // Arrange
    stubReachableSite(pendingSite());
    when(validationRateLimiter.tryAcquire(USERNAME)).thenReturn(false);

    // Act
    var result = service.handle(AssetsCommandFixtures.retrySiteGeocodingCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("TOO_MANY_REQUESTS");
    verifyNoInteractions(geocodingPort);
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(RetrySiteGeocodingCommand) -> locates the site when the provider resolves inside the coverage area (AAA)")
  void handleRetryResolved() {
    // Arrange
    var site = pendingSite();
    stubReachableSite(site);
    when(validationRateLimiter.tryAcquire(USERNAME)).thenReturn(true);
    when(geocodingPort.geocode(AssetsDomainFixtures.VALID_ADDRESS, null))
        .thenReturn(resolvedAt(AssetsDomainFixtures.LIMA_LATITUDE, AssetsDomainFixtures.LIMA_LONGITUDE));
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.retrySiteGeocodingCommand());

    // Assert
    var located = ResultTestUtils.successValue(result);
    assertThat(located.getGeocodingStatus()).isEqualTo(GeocodingStatus.RESOLVED);
    assertThat(located.getLocation().latitude()).isEqualTo(AssetsDomainFixtures.LIMA_LATITUDE);
    assertThat(located.isLocated()).isTrue();
  }

  @Test
  @DisplayName("handle(RetrySiteGeocodingCommand) -> records NOT_FOUND / OUT_OF_COVERAGE when the provider resolves elsewhere (AAA)")
  void handleRetryResolvedOutOfCoverage() {
    // Arrange
    var site = pendingSite();
    stubReachableSite(site);
    when(validationRateLimiter.tryAcquire(USERNAME)).thenReturn(true);
    when(geocodingPort.geocode(anyString(), any()))
        .thenReturn(resolvedAt(AssetsDomainFixtures.OUT_OF_COVERAGE_LATITUDE, AssetsDomainFixtures.OUT_OF_COVERAGE_LONGITUDE));
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.retrySiteGeocodingCommand());

    // Assert
    var outcome = ResultTestUtils.successValue(result);
    assertThat(outcome.getGeocodingStatus()).isEqualTo(GeocodingStatus.NOT_FOUND);
    assertThat(outcome.getGeocodingFailureReason()).isEqualTo(GeocodingFailureReason.OUT_OF_COVERAGE);
    assertThat(outcome.getLocation()).isNull();
  }

  @Test
  @DisplayName("handle(RetrySiteGeocodingCommand) -> records NOT_FOUND / ADDRESS_NOT_FOUND when the provider matches nothing (AAA)")
  void handleRetryNotFound() {
    // Arrange
    var site = AssetsDomainFixtures.siteBuilder().unlocated(GeocodingStatus.FAILED, GeocodingFailureReason.UNKNOWN).build();
    stubReachableSite(site);
    when(validationRateLimiter.tryAcquire(USERNAME)).thenReturn(true);
    when(geocodingPort.geocode(anyString(), any())).thenReturn(GeocodingResult.notFound());
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.retrySiteGeocodingCommand());

    // Assert
    var outcome = ResultTestUtils.successValue(result);
    assertThat(outcome.getGeocodingStatus()).isEqualTo(GeocodingStatus.NOT_FOUND);
    assertThat(outcome.getGeocodingFailureReason()).isEqualTo(GeocodingFailureReason.ADDRESS_NOT_FOUND);
  }

  @Test
  @DisplayName("handle(RetrySiteGeocodingCommand) -> leaves the site PENDING and queues a background attempt on a transient failure (AAA)")
  void handleRetryTransientFailure() {
    // Arrange
    var site = pendingSite();
    stubReachableSite(site);
    when(validationRateLimiter.tryAcquire(USERNAME)).thenReturn(true);
    when(geocodingPort.geocode(anyString(), any())).thenReturn(GeocodingResult.transientFailure());
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.retrySiteGeocodingCommand());

    // Assert
    var outcome = ResultTestUtils.successValue(result);
    assertThat(outcome.getGeocodingStatus()).isEqualTo(GeocodingStatus.PENDING);
    assertThat(outcome.domainEvents()).containsExactly(new SiteGeocodingPendingEvent(SITE_ID));
  }

  @Test
  @DisplayName("handle(RetrySiteGeocodingCommand) -> GEOCODING_UNAVAILABLE after storing FAILED on a permanent failure (AAA)")
  void handleRetryPermanentFailure() {
    // Arrange
    var site = pendingSite();
    stubReachableSite(site);
    when(validationRateLimiter.tryAcquire(USERNAME)).thenReturn(true);
    when(geocodingPort.geocode(anyString(), any())).thenReturn(GeocodingResult.permanentFailure());
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.retrySiteGeocodingCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("GEOCODING_UNAVAILABLE");
    assertThat(error.details()).isEqualTo("PERMANENT_FAILURE");
    assertThat(site.getGeocodingStatus()).isEqualTo(GeocodingStatus.FAILED);
    assertThat(site.getGeocodingFailureReason()).isEqualTo(GeocodingFailureReason.PROVIDER_UNAVAILABLE);
    verify(siteRepository, times(1)).save(site);
  }

  // ====================================================================================
  // handle(ResumeSiteGeocodingCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(ResumeSiteGeocodingCommand) -> SITE_NOT_FOUND when the site no longer exists (AAA)")
  void handleResumeMissingSite() {
    // Arrange
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(AssetsCommandFixtures.resumeSiteGeocodingCommand(false));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("SITE_NOT_FOUND");
    verifyNoInteractions(geocodingPort);
  }

  @Test
  @DisplayName("handle(ResumeSiteGeocodingCommand) -> returns the site untouched when it is no longer PENDING (AAA)")
  void handleResumeNoLongerPending() {
    // Arrange
    var site = AssetsDomainFixtures.site();
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(site));

    // Act
    var result = service.handle(AssetsCommandFixtures.resumeSiteGeocodingCommand(false));

    // Assert
    assertThat(ResultTestUtils.successValue(result)).isSameAs(site);
    verifyNoInteractions(geocodingPort);
    verify(siteRepository, never()).save(any(Site.class));
  }

  @Test
  @DisplayName("handle(ResumeSiteGeocodingCommand) -> locates a PENDING site when the provider resolves (AAA)")
  void handleResumeResolved() {
    // Arrange
    var site = pendingSite();
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(site));
    when(geocodingPort.geocode(anyString(), any()))
        .thenReturn(resolvedAt(AssetsDomainFixtures.CALLAO_LATITUDE, AssetsDomainFixtures.CALLAO_LONGITUDE));
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.resumeSiteGeocodingCommand(false));

    // Assert
    assertThat(ResultTestUtils.successValue(result).getGeocodingStatus()).isEqualTo(GeocodingStatus.RESOLVED);
  }

  @Test
  @DisplayName("handle(ResumeSiteGeocodingCommand) -> keeps the site PENDING on a transient failure before the final attempt (AAA)")
  void handleResumeTransientFailureNotFinal() {
    // Arrange
    var site = pendingSite();
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(site));
    when(geocodingPort.geocode(anyString(), any())).thenReturn(GeocodingResult.transientFailure());
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.resumeSiteGeocodingCommand(false));

    // Assert
    assertThat(ResultTestUtils.successValue(result).getGeocodingStatus()).isEqualTo(GeocodingStatus.PENDING);
  }

  @Test
  @DisplayName("handle(ResumeSiteGeocodingCommand) -> gives up with FAILED on a transient failure at the final attempt (AAA)")
  void handleResumeTransientFailureFinal() {
    // Arrange
    var site = pendingSite();
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(site));
    when(geocodingPort.geocode(anyString(), any())).thenReturn(GeocodingResult.transientFailure());
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.resumeSiteGeocodingCommand(true));

    // Assert
    var outcome = ResultTestUtils.successValue(result);
    assertThat(outcome.getGeocodingStatus()).isEqualTo(GeocodingStatus.FAILED);
    assertThat(outcome.getGeocodingFailureReason()).isEqualTo(GeocodingFailureReason.PROVIDER_UNAVAILABLE);
  }

  @Test
  @DisplayName("handle(ResumeSiteGeocodingCommand) -> a permanent failure ends FAILED even before the final attempt (AAA)")
  void handleResumePermanentFailure() {
    // Arrange
    var site = pendingSite();
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(site));
    when(geocodingPort.geocode(anyString(), any())).thenReturn(GeocodingResult.permanentFailure());
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.resumeSiteGeocodingCommand(false));

    // Assert
    assertThat(ResultTestUtils.successValue(result).getGeocodingStatus()).isEqualTo(GeocodingStatus.FAILED);
  }

  @Test
  @DisplayName("handle(ResumeSiteGeocodingCommand) -> records NOT_FOUND when the provider matches nothing (AAA)")
  void handleResumeNotFound() {
    // Arrange
    var site = pendingSite();
    when(siteRepository.findById(SITE_ID)).thenReturn(Optional.of(site));
    when(geocodingPort.geocode(anyString(), any())).thenReturn(GeocodingResult.notFound());
    when(siteRepository.save(site)).thenReturn(site);

    // Act
    var result = service.handle(AssetsCommandFixtures.resumeSiteGeocodingCommand(false));

    // Assert
    var outcome = ResultTestUtils.successValue(result);
    assertThat(outcome.getGeocodingStatus()).isEqualTo(GeocodingStatus.NOT_FOUND);
    assertThat(outcome.getGeocodingFailureReason()).isEqualTo(GeocodingFailureReason.ADDRESS_NOT_FOUND);
  }
}
