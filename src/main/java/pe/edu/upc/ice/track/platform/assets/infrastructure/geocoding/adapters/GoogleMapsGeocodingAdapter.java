package pe.edu.upc.ice.track.platform.assets.infrastructure.geocoding.adapters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.geocoding.GeocodingPort;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeoLocation;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingResult;

import java.time.Duration;
import java.util.List;

/**
 * {@link GeocodingPort} adapter backed by the Google Maps Geocoding API.
 *
 * <p>Every call is bounded by a {@value #TIMEOUT_SECONDS}-second connect timeout and a
 * {@value #TIMEOUT_SECONDS}-second read timeout, so a stalled provider costs one background thread
 * a few seconds and never an owner's request: this adapter is only ever called from the
 * asynchronous geocoding handler.</p>
 *
 * <p>Google answers HTTP 200 for most failures and reports the real outcome in {@code status},
 * which is mapped as follows:</p>
 * <ul>
 *   <li>{@code OK} with exactly one complete match - {@code RESOLVED};</li>
 *   <li>{@code ZERO_RESULTS}, several candidates, or a partial match - {@code NOT_FOUND}. Several
 *       candidates and partial matches are both ambiguous: pinning a technician's navigation on a
 *       guess is worse than asking the owner to make the address more precise;</li>
 *   <li>{@code OVER_QUERY_LIMIT}, {@code UNKNOWN_ERROR}, a timeout, an I/O error, HTTP 429 or
 *       5xx - {@code TRANSIENT_FAILURE}, worth retrying;</li>
 *   <li>{@code REQUEST_DENIED}, {@code OVER_DAILY_LIMIT}, {@code INVALID_REQUEST} or another HTTP
 *       4xx - {@code PERMANENT_FAILURE}, because the request itself is refused.</li>
 * </ul>
 *
 * <p>The API key travels in the query string, and Spring's client exceptions embed the full
 * request URL in their message. Those messages are therefore never logged; only status codes and
 * the underlying I/O cause are.</p>
 */
@Component
@Slf4j
public class GoogleMapsGeocodingAdapter implements GeocodingPort {

  /**
   * Upper bound, in seconds, for establishing the connection and, separately, for waiting on the
   * response.
   */
  static final int TIMEOUT_SECONDS = 5;

  private final RestClient restClient;
  private final String apiKey;

  /**
   * Constructor
   *
   * @param apiKey       the Google Maps API key
   * @param geocodingUrl the Geocoding API endpoint; overridable so a test can point it at a stub
   */
  public GoogleMapsGeocodingAdapter(
      @Value("${google.maps.api-key:YOUR_MAPS_DEMO_KEY}") String apiKey,
      @Value("${google.maps.geocoding-url:https://maps.googleapis.com/maps/api/geocode/json}") String geocodingUrl) {
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS));
    requestFactory.setReadTimeout(Duration.ofSeconds(TIMEOUT_SECONDS));
    this.restClient = RestClient.builder()
        .baseUrl(geocodingUrl)
        .requestFactory(requestFactory)
        .build();
    this.apiKey = apiKey;
  }

  // inherited javadoc
  @Override
  public GeocodingResult geocode(String address) {
    if (address == null || address.isBlank()) {
      return GeocodingResult.notFound();
    }
    try {
      // Passed as URI variables rather than literal query values so they are strictly encoded:
      // an address may legitimately contain '&', '#' or '+'.
      var response = restClient.get()
          .uri(uriBuilder -> uriBuilder
              .queryParam("address", "{address}")
              .queryParam("key", "{key}")
              .build(address, apiKey))
          .retrieve()
          .body(GoogleGeocodingResponse.class);
      return toResult(response);
    } catch (RestClientResponseException e) {
      var status = e.getStatusCode();
      if (status.is5xxServerError() || status.value() == 429) {
        log.warn("Google Maps geocoding answered HTTP {}; treating it as transient", status.value());
        return GeocodingResult.transientFailure();
      }
      log.error("Google Maps geocoding refused the request with HTTP {}", status.value());
      return GeocodingResult.permanentFailure();
    } catch (ResourceAccessException e) {
      log.warn("Google Maps geocoding unreachable or too slow ({}); treating it as transient", causeOf(e));
      return GeocodingResult.transientFailure();
    } catch (RestClientException e) {
      log.warn("Google Maps geocoding response could not be read ({}); treating it as transient", causeOf(e));
      return GeocodingResult.transientFailure();
    }
  }

  /**
   * Maps Google's status code to a result.
   *
   * @param response the parsed response; may be {@code null} for an empty body
   * @return the corresponding result
   */
  private static GeocodingResult toResult(GoogleGeocodingResponse response) {
    if (response == null || response.status() == null) {
      log.warn("Google Maps geocoding returned an empty response; treating it as transient");
      return GeocodingResult.transientFailure();
    }
    return switch (response.status()) {
      case "OK" -> toLocationResult(response.results());
      case "ZERO_RESULTS" -> GeocodingResult.notFound();
      case "OVER_QUERY_LIMIT", "UNKNOWN_ERROR" -> {
        log.warn("Google Maps geocoding answered {}; treating it as transient", response.status());
        yield GeocodingResult.transientFailure();
      }
      default -> {
        log.error("Google Maps geocoding refused the request with {}: {}",
            response.status(), response.errorMessage());
        yield GeocodingResult.permanentFailure();
      }
    };
  }

  /**
   * Picks the location out of an {@code OK} response, refusing anything ambiguous.
   *
   * @param results the candidate places
   * @return {@code RESOLVED} for a single complete match, {@code NOT_FOUND} otherwise
   */
  private static GeocodingResult toLocationResult(List<GoogleGeocodingResponse.Result> results) {
    if (results == null || results.isEmpty()) {
      return GeocodingResult.notFound();
    }
    if (results.size() > 1) {
      log.info("Google Maps geocoding matched {} places; treating the address as ambiguous", results.size());
      return GeocodingResult.notFound();
    }
    var result = results.getFirst();
    if (Boolean.TRUE.equals(result.partialMatch())) {
      log.info("Google Maps geocoding matched only part of the address; treating it as ambiguous");
      return GeocodingResult.notFound();
    }
    var point = result.geometry() == null ? null : result.geometry().location();
    if (point == null || point.lat() == null || point.lng() == null) {
      log.warn("Google Maps geocoding returned a match without coordinates");
      return GeocodingResult.notFound();
    }
    try {
      return GeocodingResult.resolved(new GeoLocation(point.lat(), point.lng(), result.placeId()));
    } catch (IllegalArgumentException e) {
      log.warn("Google Maps geocoding returned unusable coordinates: {}", e.getMessage());
      return GeocodingResult.notFound();
    }
  }

  /**
   * Describes why a call failed without leaking the request URL, and with it the API key, that
   * Spring's own exception messages embed.
   *
   * @param e the client exception
   * @return the type and message of the underlying cause, or only the exception type when there is
   *         no underlying cause
   */
  private static String causeOf(RestClientException e) {
    var cause = NestedExceptionUtils.getMostSpecificCause(e);
    return cause == e
        ? e.getClass().getSimpleName()
        : "%s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage());
  }
}
