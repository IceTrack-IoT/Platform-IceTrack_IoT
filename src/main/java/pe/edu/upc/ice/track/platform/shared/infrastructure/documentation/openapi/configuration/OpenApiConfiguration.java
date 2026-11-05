package pe.edu.upc.ice.track.platform.shared.infrastructure.documentation.openapi.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures the OpenAPI specification exposed by the platform.
 */
@Configuration
public class OpenApiConfiguration {
  // Properties
  @Value("${spring.application.name}")
  String applicationName;

  @Value("${documentation.application.description}")
  String applicationDescription;

  @Value("${documentation.application.version}")
  String applicationVersion;

  // Methods

  /**
   * Builds the schema resolver used to generate the OpenAPI components.
   *
   * <p>swagger-core introspects resources with its own Jackson 2 mapper, which does not inherit the
   * platform-wide Jackson 3 {@code SNAKE_CASE} strategy. Registering this resolver keeps the
   * properties shown in Swagger UI in {@code snake_case}, matching the JSON the API actually
   * produces, without a per-field {@code @JsonProperty}.</p>
   *
   * @param springDocConfigProperties the springdoc configuration, used to keep the OpenAPI version
   * @return the model resolver configured with the platform naming strategy
   */
  @Bean
  public ModelResolver snakeCaseModelResolver(SpringDocConfigProperties springDocConfigProperties) {
    boolean openApi31 = springDocConfigProperties.isOpenapi31();
    ObjectMapper mapper = (openApi31 ? Json31.mapper() : Json.mapper()).copy();
    mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    return new ModelResolver(mapper).openapi31(openApi31);
  }

  /**
   * Builds the OpenAPI document used by Swagger UI and client generation tools.
   *
   * @return configured OpenAPI descriptor
   */
  @Bean
  public OpenAPI learningPlatformOpenApi() {

    // General configuration
    var openApi = new OpenAPI();
    openApi
        .info(new Info()
            .title(this.applicationName)
            .description(this.applicationDescription)
            .version(this.applicationVersion)
            .contact(new Contact()
                .name("IceTrack Center Support")
                .email("support@ice-track.com")
                .url("https://ice-track.com/support"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
        .externalDocs(new ExternalDocumentation()
            .description("IceTrack Platform wiki Documentation")
            .url("https://ice-track-platform.wiki.github.io/docs"));

    // Add server configurations
    openApi.servers(List.of(
        new Server()
            .url("http://localhost:8080")
            .description("Local Development Environment"),
        new Server()
            .url("https://platform-icetrackiot-production.up.railway.app/")
            .description("Production Environment")
    ));

    // Add a security scheme
    final String securitySchemeName = "bearerAuth";

    openApi.addSecurityItem(new SecurityRequirement()
            .addList(securitySchemeName))
        .components(new Components()
            .addSecuritySchemes(securitySchemeName,
                new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT Bearer token for API authentication")));

    return openApi;
  }
}
