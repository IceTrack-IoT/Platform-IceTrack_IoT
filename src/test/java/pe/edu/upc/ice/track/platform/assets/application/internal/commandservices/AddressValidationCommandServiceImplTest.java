package pe.edu.upc.ice.track.platform.assets.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation.AddressValidationTokenPort;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation.ValidationRateLimiterPort;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.geocoding.GeocodingPort;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.CoverageArea;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeoLocation;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingResult;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.VerifiedAddress;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsCommandFixtures;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;
import pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class AddressValidationCommandServiceImplTest {

  private static final String ISSUED_TOKEN = "signed-token";

  @Mock
  private GeocodingPort geocodingPort;
  @Mock
  private AddressValidationTokenPort tokenPort;
  @Mock
  private ValidationRateLimiterPort rateLimiter;
  @Mock
  private AssetsExternalOwnerService assetsExternalOwnerService;

  @InjectMocks
  private AddressValidationCommandServiceImpl service;

  private void stubOwnerWithinQuota() {
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
    when(rateLimiter.tryAcquire(USERNAME)).thenReturn(true);
  }

  @Test
  @DisplayName("handle(ValidateAddressCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleNoOwnerProfile() {
    // Arrange
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(AssetsCommandFixtures.validateAddressCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(rateLimiter, geocodingPort, tokenPort);
  }

  @Test
  @DisplayName("handle(ValidateAddressCommand) -> TOO_MANY_REQUESTS without querying the provider when over quota (AAA)")
  void handleRateLimited() {
    // Arrange
    when(assetsExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
    when(rateLimiter.tryAcquire(USERNAME)).thenReturn(false);

    // Act
    var result = service.handle(AssetsCommandFixtures.validateAddressCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("TOO_MANY_REQUESTS");
    verifyNoInteractions(geocodingPort, tokenPort);
  }

  @Test
  @DisplayName("handle(ValidateAddressCommand) -> issues a token for an address resolved in Metropolitan Lima (AAA)")
  void handleResolvedInLima() {
    // Arrange
    var captor = ArgumentCaptor.forClass(VerifiedAddress.class);
    stubOwnerWithinQuota();
    when(geocodingPort.geocode(VALID_ADDRESS, null)).thenReturn(
        GeocodingResult.resolved(AssetsDomainFixtures.geoLocation(), "Formatted Address, Lima", VALID_DISTRICT));
    when(tokenPort.issue(eq(OWNER_ID), any(VerifiedAddress.class))).thenReturn(ISSUED_TOKEN);

    // Act
    var result = service.handle(AssetsCommandFixtures.validateAddressCommand());

    // Assert
    var validation = ResultTestUtils.successValue(result);
    assertThat(validation.validationToken()).isEqualTo(ISSUED_TOKEN);
    assertThat(validation.verified().formattedAddress()).isEqualTo("Formatted Address, Lima");
    assertThat(validation.verified().area()).isEqualTo(CoverageArea.LIMA_METROPOLITANA);
    assertThat(validation.verified().district()).isEqualTo(VALID_DISTRICT);
    assertThat(validation.verified().placeId()).isEqualTo(VALID_PLACE_ID);
    verify(tokenPort, times(1)).issue(eq(OWNER_ID), captor.capture());
    assertThat(captor.getValue()).isEqualTo(validation.verified());
  }

  @Test
  @DisplayName("handle(ValidateAddressCommand) -> classifies an address resolved inside Callao as CALLAO (AAA)")
  void handleResolvedInCallao() {
    // Arrange
    stubOwnerWithinQuota();
    var callao = new GeoLocation(AssetsDomainFixtures.CALLAO_LATITUDE, AssetsDomainFixtures.CALLAO_LONGITUDE, null);
    when(geocodingPort.geocode(anyAddress(), any())).thenReturn(GeocodingResult.resolved(callao, "Av. Callao 1", "Callao"));
    when(tokenPort.issue(eq(OWNER_ID), any(VerifiedAddress.class))).thenReturn(ISSUED_TOKEN);

    // Act
    var result = service.handle(AssetsCommandFixtures.validateAddressCommand());

    // Assert
    assertThat(ResultTestUtils.successValue(result).verified().area()).isEqualTo(CoverageArea.CALLAO);
  }

  @Test
  @DisplayName("handle(ValidateAddressCommand) -> falls back to the trimmed requested address when the provider gives none (AAA)")
  void handleResolvedWithoutFormattedAddress() {
    // Arrange
    stubOwnerWithinQuota();
    when(geocodingPort.geocode(anyAddress(), any()))
        .thenReturn(GeocodingResult.resolved(AssetsDomainFixtures.geoLocation(), null, null));
    when(tokenPort.issue(eq(OWNER_ID), any(VerifiedAddress.class))).thenReturn(ISSUED_TOKEN);

    // Act
    var result = service.handle(AssetsCommandFixtures.validateAddressCommand("  Av. Test 1  ", null));

    // Assert
    var verified = ResultTestUtils.successValue(result).verified();
    assertThat(verified.formattedAddress()).isEqualTo("Av. Test 1");
    assertThat(verified.district()).isNull();
  }

  @Test
  @DisplayName("handle(ValidateAddressCommand) -> forwards the place id picked by the owner to the provider (AAA)")
  void handleForwardsPlaceId() {
    // Arrange
    stubOwnerWithinQuota();
    when(geocodingPort.geocode(VALID_ADDRESS, VALID_PLACE_ID)).thenReturn(GeocodingResult.notFound());

    // Act
    service.handle(AssetsCommandFixtures.validateAddressCommandWithPlaceId());

    // Assert
    verify(geocodingPort, times(1)).geocode(VALID_ADDRESS, VALID_PLACE_ID);
  }

  @Test
  @DisplayName("handle(ValidateAddressCommand) -> ADDRESS_OUT_OF_COVERAGE and no token when resolved outside the service area (AAA)")
  void handleResolvedOutOfCoverage() {
    // Arrange
    stubOwnerWithinQuota();
    var newYork = new GeoLocation(
        AssetsDomainFixtures.OUT_OF_COVERAGE_LATITUDE, AssetsDomainFixtures.OUT_OF_COVERAGE_LONGITUDE, null);
    when(geocodingPort.geocode(anyAddress(), any())).thenReturn(GeocodingResult.resolved(newYork, "New York", null));

    // Act
    var result = service.handle(AssetsCommandFixtures.validateAddressCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ADDRESS_OUT_OF_COVERAGE");
    verifyNoInteractions(tokenPort);
  }

  @Test
  @DisplayName("handle(ValidateAddressCommand) -> ADDRESS_NOT_VERIFIED listing at most five candidates, one per line (AAA)")
  void handleNotFoundLimitsCandidates() {
    // Arrange
    stubOwnerWithinQuota();
    when(geocodingPort.geocode(anyAddress(), any()))
        .thenReturn(GeocodingResult.notFound(List.of("c1", "c2", "c3", "c4", "c5", "c6", "c7")));

    // Act
    var result = service.handle(AssetsCommandFixtures.validateAddressCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("ADDRESS_NOT_VERIFIED");
    assertThat(error.details()).isEqualTo("c1\nc2\nc3\nc4\nc5");
    verifyNoInteractions(tokenPort);
  }

  @Test
  @DisplayName("handle(ValidateAddressCommand) -> ADDRESS_NOT_VERIFIED with empty details when there are no candidates (AAA)")
  void handleNotFoundWithoutCandidates() {
    // Arrange
    stubOwnerWithinQuota();
    when(geocodingPort.geocode(anyAddress(), any())).thenReturn(GeocodingResult.notFound());

    // Act
    var result = service.handle(AssetsCommandFixtures.validateAddressCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("ADDRESS_NOT_VERIFIED");
    assertThat(error.details()).isEmpty();
  }

  @Test
  @DisplayName("handle(ValidateAddressCommand) -> GEOCODING_UNAVAILABLE naming a transient failure (AAA)")
  void handleTransientFailure() {
    // Arrange
    stubOwnerWithinQuota();
    when(geocodingPort.geocode(anyAddress(), any())).thenReturn(GeocodingResult.transientFailure());

    // Act
    var result = service.handle(AssetsCommandFixtures.validateAddressCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("GEOCODING_UNAVAILABLE");
    assertThat(error.details()).isEqualTo("TRANSIENT_FAILURE");
  }

  @Test
  @DisplayName("handle(ValidateAddressCommand) -> GEOCODING_UNAVAILABLE naming a permanent failure (AAA)")
  void handlePermanentFailure() {
    // Arrange
    stubOwnerWithinQuota();
    when(geocodingPort.geocode(anyAddress(), any())).thenReturn(GeocodingResult.permanentFailure());

    // Act
    var result = service.handle(AssetsCommandFixtures.validateAddressCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("GEOCODING_UNAVAILABLE");
    assertThat(error.details()).isEqualTo("PERMANENT_FAILURE");
    verifyNoInteractions(tokenPort);
  }

  private static String anyAddress() {
    return org.mockito.ArgumentMatchers.anyString();
  }
}
