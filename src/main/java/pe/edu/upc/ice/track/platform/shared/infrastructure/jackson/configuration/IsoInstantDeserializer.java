package pe.edu.upc.ice.track.platform.shared.infrastructure.jackson.configuration;

import pe.edu.upc.ice.track.platform.shared.infrastructure.time.IsoInstants;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

import java.time.Instant;

/**
 * Reads every {@link Instant} of a request body with {@link IsoInstants#parse}: an ISO 8601 string with
 * {@code Z} or an explicit offset. Jackson's own reader would also take a bare number, or a string of
 * digits, as seconds since the epoch, and a zone-less value elsewhere in the API used to be read in the
 * server's zone; both are refused here, and the request is answered {@code 400}.
 */
class IsoInstantDeserializer extends StdScalarDeserializer<Instant> {

  IsoInstantDeserializer() {
    super(Instant.class);
  }

  @Override
  public Instant deserialize(JsonParser parser, DeserializationContext context) {
    if (!parser.hasToken(JsonToken.VALUE_STRING)) {
      return (Instant) context.handleUnexpectedToken(Instant.class, parser);
    }
    var text = parser.getString();
    try {
      return IsoInstants.parse(text);
    } catch (IllegalArgumentException e) {
      return (Instant) context.handleWeirdStringValue(Instant.class, text, IsoInstants.EXPECTED_FORMAT);
    }
  }
}
