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
import java.util.Map;
import java.util.Objects;

/**
 * {@link GeocodingPort} adapter backed by the Google Maps Geocoding API.
 *
 * <p>The lookup runs synchronously inside the owner's validation request, so its time is bounded
 * strictly: a connection may take at most {@value #CONNECT_TIMEOUT_SECONDS} seconds and the wait for
 * the response at most {@value #READ_TIMEOUT_SECONDS} seconds, which keeps the whole call within
 * {@value #TOTAL_TIMEOUT_SECONDS} seconds.</p>
 *
 * <p>Google answers HTTP 200 for most failures and reports the real outcome in {@code status}:</p>
 * <ul>
 *   <li>{@code OK} with exactly one complete match - {@code RESOLVED};</li>
 *   <li>{@code ZERO_RESULTS}, several candidates, or a partial match - {@code NOT_FOUND}, with the
 *       candidate addresses so the owner can choose one;</li>
 *   <li>{@code OVER_QUERY_LIMIT}, {@code UNKNOWN_ERROR}, a timeout, an I/O error, HTTP 429 or 5xx -
 *       {@code TRANSIENT_FAILURE};</li>
 *   <li>{@code REQUEST_DENIED}, {@code OVER_DAILY_LIMIT}, {@code INVALID_REQUEST} or another HTTP 4xx
 *       - {@code PERMANENT_FAILURE}.</li>
 * </ul>
 *
 * <p>The API key travels in the query string, and Spring's client exceptions embed the full request
 * URL in their message. Those messages are therefore never logged; only status codes and the
 * underlying I/O cause are.</p>
 */
@Component
@Slf4j
public class GoogleMapsGeocodingAdapter implements GeocodingPort {

  /** Upper bound, in seconds, for establishing the connection. */
  static final int CONNECT_TIMEOUT_SECONDS = 2;

  /** Upper bound, in seconds, for waiting on the response. */
  static final int READ_TIMEOUT_SECONDS = 3;

  /** Worst-case duration of one lookup: connection plus response. */
  static final int TOTAL_TIMEOUT_SECONDS = CONNECT_TIMEOUT_SECONDS + READ_TIMEOUT_SECONDS;

  private static final int MAX_CANDIDATES = GeocodingResult.MAX_CANDIDATES;

  private static final List<String> DISTRICT_TYPES = List.of(
      "sublocality_level_1", "locality", "administrative_area_level_3");

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
    requestFactory.setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS));
    requestFactory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS));
    this.restClient = RestClient.builder()
        .baseUrl(geocodingUrl)
        .requestFactory(requestFactory)
        .build();
    this.apiKey = apiKey;
  }

  // inherited javadoc
  @Override
  public GeocodingResult geocode(String address, String placeId) {
    if (placeId == null && (address == null || address.isBlank())) {
      return GeocodingResult.notFound();
    }
    try {
      // URI variables keep the values strictly encoded: an address may contain '&', '#' or '+'.
      var response = restClient.get()
          .uri(uriBuilder -> placeId != null
              ? uriBuilder.queryParam("place_id", "{placeId}").queryParam("key", "{key}")
                  .build(Map.of("placeId", placeId, "key", apiKey))
              : uriBuilder.queryParam("address", "{address}").queryParam("key", "{key}")
                  .build(Map.of("address", address, "key", apiKey)))
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
   * @return {@code RESOLVED} for a single complete match, {@code NOT_FOUND} with candidates otherwise
   */
  private static GeocodingResult toLocationResult(List<GoogleGeocodingResponse.Result> results) {
    if (results == null || results.isEmpty()) {
      return GeocodingResult.notFound();
    }
    var candidates = results.stream()
        .map(GoogleGeocodingResponse.Result::formattedAddress)
        .filter(Objects::nonNull)
        .limit(MAX_CANDIDATES)
        .toList();
    if (results.size() > 1) {
      log.info("Google Maps geocoding matched {} places; treating the address as ambiguous", results.size());
      return GeocodingResult.notFound(candidates);
    }
    var result = results.getFirst();
    if (Boolean.TRUE.equals(result.partialMatch())) {
      log.info("Google Maps geocoding matched only part of the address; treating it as ambiguous");
      return GeocodingResult.notFound(candidates);
    }
    var point = result.geometry() == null ? null : result.geometry().location();
    if (point == null || point.lat() == null || point.lng() == null) {
      log.warn("Google Maps geocoding returned a match without coordinates");
      return GeocodingResult.notFound(candidates);
    }
    try {
      return GeocodingResult.resolved(
          new GeoLocation(point.lat(), point.lng(), result.placeId()),
          result.formattedAddress(),
          districtOf(result.addressComponents()));
    } catch (IllegalArgumentException e) {
      log.warn("Google Maps geocoding returned unusable coordinates: {}", e.getMessage());
      return GeocodingResult.notFound(candidates);
    }
  }

  /**
   * Reads the district name from the address components, preferring the most specific kind.
   *
   * @param components the address components of the match; may be {@code null}
   * @return the district name, or {@code null} when Google named none
   */
  private static String districtOf(List<GoogleGeocodingResponse.AddressComponent> components) {
    if (components == null) {
      return null;
    }
    for (var type : DISTRICT_TYPES) {
      var match = components.stream()
          .filter(component -> component.types() != null && component.types().contains(type))
          .map(GoogleGeocodingResponse.AddressComponent::longName)
          .filter(Objects::nonNull)
          .findFirst();
      if (match.isPresent()) {
        return match.get();
      }
    }
    return null;
  }

  /**
   * Describes why a call failed without leaking the request URL, and with it the API key.
   *
   * @param e the client exception
   * @return the type and message of the underlying cause, or only the exception type
   */
  private static String causeOf(RestClientException e) {
    var cause = NestedExceptionUtils.getMostSpecificCause(e);
    return cause == e
        ? e.getClass().getSimpleName()
        : "%s: %s".formatted(cause.getClass().getSimpleName(), cause.getMessage());
  }
}
