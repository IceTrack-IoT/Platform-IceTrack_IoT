package pe.edu.upc.ice.track.platform.shared.infrastructure.jackson.configuration;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.PropertyNamingStrategies;

/**
 * Configures Jackson to use {@code snake_case} for all REST resource fields.
 *
 * <p>Java record components stay in {@code camelCase} following Java conventions,
 * while the JSON representation exposed by every endpoint uses {@code snake_case}
 * (for example {@code fullName} becomes {@code full_name}, {@code idToken} becomes
 * {@code id_token} and {@code certificationNumber} becomes {@code certification_number}).
 * Centralizing the strategy here keeps every current and future resource standardized
 * without per-field annotations. It mirrors the
 * {@code spring.jackson.property-naming-strategy=SNAKE_CASE} property as an explicit,
 * type-safe guarantee.</p>
 */
@Configuration
public class JacksonConfiguration {

  @Bean
  public JsonMapperBuilderCustomizer snakeCaseJsonMapperBuilderCustomizer() {
    return builder -> builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
  }
}
