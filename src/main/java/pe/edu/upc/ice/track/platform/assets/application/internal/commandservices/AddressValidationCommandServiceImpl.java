package pe.edu.upc.ice.track.platform.assets.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.AddressValidationCommandService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.acl.AssetsExternalOwnerService;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation.AddressValidationTokenPort;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation.ValidationRateLimiterPort;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.geocoding.GeocodingPort;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ValidateAddressCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.policies.CoveragePolicy;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.AddressValidationResult;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingOutcome;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingResult;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.VerifiedAddress;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.List;

/**
 * Default {@link AddressValidationCommandService}.
 *
 * <p>The steps run in a fixed order, and each one can end the request: the caller is resolved to an
 * owner, the caller's quota is charged, the provider is queried (synchronously, with a bounded
 * timeout), the answer is checked against the service coverage, and only then is a token issued.
 * The quota is charged before the provider is called, so a flood of requests cannot reach Google
 * through this endpoint.</p>
 *
 * <p>Nothing is saved. Whatever the provider answers, the site table is untouched.</p>
 */
@Service
@Slf4j
public class AddressValidationCommandServiceImpl implements AddressValidationCommandService {

  /** Candidates offered to the owner when an address is ambiguous. */
  static final int MAX_CANDIDATES = GeocodingResult.MAX_CANDIDATES;

  private final GeocodingPort geocodingPort;
  private final AddressValidationTokenPort tokenPort;
  private final ValidationRateLimiterPort rateLimiter;
  private final AssetsExternalOwnerService assetsExternalOwnerService;

  /**
   * Constructor
   *
   * @param geocodingPort               the {@link GeocodingPort} used to verify addresses
   * @param tokenPort                   the port issuing the validation token
   * @param rateLimiter                 the port limiting validations per caller
   * @param assetsExternalOwnerService  the owner resolution ACL
   */
  public AddressValidationCommandServiceImpl(
      GeocodingPort geocodingPort,
      AddressValidationTokenPort tokenPort,
      ValidationRateLimiterPort rateLimiter,
      AssetsExternalOwnerService assetsExternalOwnerService) {
    this.geocodingPort = geocodingPort;
    this.tokenPort = tokenPort;
    this.rateLimiter = rateLimiter;
    this.assetsExternalOwnerService = assetsExternalOwnerService;
  }

  // inherited javadoc
  @Override
  public Result<AddressValidationResult, ApplicationError> handle(ValidateAddressCommand command) {
    var ownerId = assetsExternalOwnerService.fetchOwnerIdByUsername(command.username());
    if (ownerId.isEmpty()) {
      return Result.failure(ApplicationError.accessDenied(
          "The authenticated account has no owner profile, so it cannot verify addresses"));
    }
    if (!rateLimiter.tryAcquire(command.username())) {
      return Result.failure(ApplicationError.tooManyRequests(
          "More than 20 address validations within one minute; try again shortly"));
    }

    var result = geocodingPort.geocode(command.address(), command.placeId());
    return switch (result.outcome()) {
      case RESOLVED -> verified(ownerId.get(), command.address(), result);
      case NOT_FOUND -> Result.failure(notVerified(result.candidates()));
      case TRANSIENT_FAILURE, PERMANENT_FAILURE -> Result.failure(unavailable(result.outcome()));
    };
  }

  /**
   * Checks the coverage of a resolved answer and issues its token.
   *
   * @param ownerId the owner the token is issued to
   * @param result  the resolved lookup
   * @return the validation result, or an out-of-coverage failure
   */
  private Result<AddressValidationResult, ApplicationError> verified(Long ownerId, String requestedAddress, GeocodingResult result) {
    var location = result.location();
    var area = CoveragePolicy.areaOf(location.latitude(), location.longitude());
    if (area.isEmpty()) {
      log.info("Address validation resolved outside Metropolitan Lima and Callao; refused");
      return Result.failure(new ApplicationError(
          "ADDRESS_OUT_OF_COVERAGE",
          "The address is outside the service area",
          "The platform serves Metropolitan Lima and the Constitutional Province of Callao only"));
    }
    var formatted = result.formattedAddress() != null ? result.formattedAddress() : requestedAddress.trim();
    var address = new VerifiedAddress(formatted, location.placeId(), location, result.district(), area.get());
    var token = tokenPort.issue(ownerId, address);
    return Result.success(new AddressValidationResult(address, token));
  }

  /**
   * Builds the failure for an address that matched nothing, or more than one place.
   *
   * @param candidates the formatted candidate addresses, possibly empty
   * @return the failure, with the candidates in its details, one per line
   */
  private static ApplicationError notVerified(List<String> candidates) {
    var offered = candidates.stream().limit(MAX_CANDIDATES).toList();
    return new ApplicationError(
        "ADDRESS_NOT_VERIFIED",
        "The address could not be verified as a single location",
        String.join("\n", offered));
  }

  /**
   * Builds the failure for a provider that could not answer.
   *
   * @param outcome the failed outcome, transient or permanent
   * @return the failure; its details are never shown to the client
   */
  private static ApplicationError unavailable(GeocodingOutcome outcome) {
    return new ApplicationError(
        "GEOCODING_UNAVAILABLE",
        "The address verification service is unavailable",
        outcome.name());
  }
}
