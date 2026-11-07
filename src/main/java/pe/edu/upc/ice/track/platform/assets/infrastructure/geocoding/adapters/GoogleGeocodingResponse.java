package pe.edu.upc.ice.track.platform.assets.infrastructure.geocoding.adapters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The subset of a Google Maps Geocoding API response that {@link GoogleMapsGeocodingAdapter}
 * reads.
 *
 * <p>Every property is named explicitly, so the platform-wide {@code SNAKE_CASE} naming strategy
 * plays no part in how Google's payload is read, and unknown properties are ignored so that Google
 * adding a field never breaks a lookup.</p>
 *
 * @param status       Google's own outcome code, such as {@code OK} or {@code ZERO_RESULTS}
 * @param results      the candidate places, empty unless the status is {@code OK}
 * @param errorMessage Google's explanation when it refuses a request, otherwise {@code null}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleGeocodingResponse(
    @JsonProperty("status") String status,
    @JsonProperty("results") List<Result> results,
    @JsonProperty("error_message") String errorMessage) {

  /**
   * One candidate place.
   *
   * @param formattedAddress  the normalized address Google writes for the place
   * @param addressComponents the parts of the address, used to read the district
   * @param geometry          where the place is
   * @param placeId           Google's identifier for the place
   * @param partialMatch      {@code true} when Google matched only part of the address
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Result(
      @JsonProperty("formatted_address") String formattedAddress,
      @JsonProperty("address_components") List<AddressComponent> addressComponents,
      @JsonProperty("geometry") Geometry geometry,
      @JsonProperty("place_id") String placeId,
      @JsonProperty("partial_match") Boolean partialMatch) {
  }

  /**
   * One part of an address, such as a street, a district or a province.
   *
   * @param longName the full name of the part
   * @param types    the kinds of part it is, such as {@code locality}
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record AddressComponent(
      @JsonProperty("long_name") String longName,
      @JsonProperty("types") List<String> types) {
  }

  /**
   * The geometry of a candidate place.
   *
   * @param location the point Google places it at
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Geometry(@JsonProperty("location") Location location) {
  }

  /**
   * A point, in degrees.
   *
   * @param lat the latitude
   * @param lng the longitude
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Location(@JsonProperty("lat") Double lat, @JsonProperty("lng") Double lng) {
  }
}
