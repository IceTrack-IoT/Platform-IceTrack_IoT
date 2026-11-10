package pe.edu.upc.ice.track.platform.shared.infrastructure.web.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pe.edu.upc.ice.track.platform.shared.infrastructure.time.IsoInstants;

import java.time.Instant;
import java.util.Locale;

/**
 * Binds every {@link Instant} query parameter and path variable with {@link IsoInstants#parse}, the
 * rule request bodies follow too.
 *
 * <p>Spring's default binding would read a bare number as milliseconds since the epoch, while the JSON
 * reader would have read it as seconds; one rule for both keeps a range filter and a body field from
 * meaning different moments. A refused value is answered {@code 400 VALIDATION_ERROR}. A {@code +} in an
 * offset must be sent percent-encoded ({@code %2B}), as in any query string.</p>
 */
@Configuration
public class TemporalFormatConfiguration implements WebMvcConfigurer {

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addFormatterForFieldType(Instant.class, new Formatter<Instant>() {
      @Override
      public Instant parse(String text, Locale locale) {
        return IsoInstants.parse(text);
      }

      @Override
      public String print(Instant instant, Locale locale) {
        return instant.toString();
      }
    });
  }
}
