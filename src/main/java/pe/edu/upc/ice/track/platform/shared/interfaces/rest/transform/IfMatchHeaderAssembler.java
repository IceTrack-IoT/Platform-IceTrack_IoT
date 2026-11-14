package pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform;

/**
 * Reads the resource version a client sends in the {@code If-Match} request header.
 *
 * <p>The header carries the {@code version} of the resource the client edited, quoted as an entity
 * tag ({@code "3"}) or bare ({@code 3}). A weak prefix ({@code W/"3"}) is accepted and ignored.
 * Anything else is rejected with an {@link IllegalArgumentException}, which the global exception
 * handler answers as {@code 400 VALIDATION_ERROR}.</p>
 */
public final class IfMatchHeaderAssembler {

  /** Name of the request header carrying the expected version. */
  public static final String IF_MATCH_HEADER = "If-Match";

  private IfMatchHeaderAssembler() {
  }

  /**
   * Converts the header value into the expected version.
   *
   * @param headerValue the raw header value; may be {@code null}
   * @return the expected version, or {@code null} when the header is absent or blank
   * @throws IllegalArgumentException when the header does not carry a non-negative integer version
   */
  public static Long toExpectedVersion(String headerValue) {
    if (headerValue == null || headerValue.isBlank()) {
      return null;
    }
    var value = headerValue.trim();
    if (value.startsWith("W/")) {
      value = value.substring(2).trim();
    }
    if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
      value = value.substring(1, value.length() - 1).trim();
    }
    try {
      var version = Long.parseLong(value);
      if (version < 0) {
        throw new NumberFormatException("negative version");
      }
      return version;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "If-Match must carry the version of the resource being updated, such as \"3\"");
    }
  }
}
