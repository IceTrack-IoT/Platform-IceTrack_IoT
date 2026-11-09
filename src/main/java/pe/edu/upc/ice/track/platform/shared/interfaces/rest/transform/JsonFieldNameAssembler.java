package pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Translates the Java path of a bound field, as Bean Validation reports it, into the name the client
 * sent it under.
 *
 * <p>Request bodies are read with the {@code SNAKE_CASE} naming strategy, so a violation reported on
 * {@code minCelsius} concerns the JSON property {@code min_celsius}. Every segment of a nested path is
 * translated and indexes are kept: {@code photoEvidence[2]} becomes {@code photo_evidence[2]} and
 * {@code pin.latitude} stays {@code pin.latitude}.</p>
 */
public final class JsonFieldNameAssembler {

  private static final Pattern WORD_BOUNDARY = Pattern.compile("([a-z0-9])([A-Z])");

  private JsonFieldNameAssembler() {
  }

  /**
   * @param javaFieldPath the field path reported by the validator, such as {@code minCelsius}
   * @return the matching JSON property path, such as {@code min_celsius}; {@code null} for {@code null}
   */
  public static String toJsonFieldName(String javaFieldPath) {
    if (javaFieldPath == null) {
      return null;
    }
    return WORD_BOUNDARY.matcher(javaFieldPath).replaceAll("$1_$2").toLowerCase(Locale.ROOT);
  }
}
