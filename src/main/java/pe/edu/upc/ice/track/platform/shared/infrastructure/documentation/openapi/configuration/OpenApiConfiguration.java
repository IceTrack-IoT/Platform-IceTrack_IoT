package pe.edu.upc.ice.track.platform.shared.infrastructure.documentation.openapi.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configures the OpenAPI specification exposed by the platform.
 */
@Configuration
public class OpenApiConfiguration {
  /** Name of the security scheme edge endpoints reference through {@code @SecurityRequirement}. */
  public static final String DEVICE_API_KEY_SCHEME = "deviceApiKey";

  /** Name of the standard error body schema, declared by {@code ErrorResource}. */
  static final String ERROR_SCHEMA_NAME = "ErrorResponse";

  /** Status codes whose answers always carry the standard error body. */
  private static final List<String> STANDARD_ERROR_STATUS_CODES = List.of("400", "401", "429", "500", "503");

  private static final String RETRY_AFTER_HEADER = "Retry-After";

  /** Every route below it is public, except {@link #AUTHENTICATED_ME_PATH}. */
  private static final String AUTHENTICATION_PATH_PREFIX = "/api/v1/authentication/";
  private static final String AUTHENTICATED_ME_PATH = "/api/v1/authentication/me";

  /** Opened with a stream ticket rather than a bearer token. */
  private static final String TELEMETRY_STREAM_PATH = "/api/v1/telemetry/stream";

  /**
   * Routes answering {@code 429}: the credential checks throttled by the sign-in rate limit filter,
   * address verification (20 per owner per minute) and stream ticket issuance (pending tickets).
   */
  private static final Set<String> RATE_LIMITED_PATHS = Set.of(
      "/api/v1/authentication/sign-in/local",
      "/api/v1/authentication/google/verify",
      "/api/v1/authentication/google/link",
      "/api/v1/assets/sites/validate-address",
      "/api/v1/telemetry/stream-ticket");

  /** Routes calling the geocoding service synchronously, which answer {@code 503} when it fails. */
  private static final Set<String> GEOCODING_PATHS = Set.of("/api/v1/assets/sites/validate-address");

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

    // Security schemes: user JWT for the application API, device API key for the edge routes
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
                    .description("JWT Bearer token for API authentication"))
            .addSecuritySchemes(DEVICE_API_KEY_SCHEME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("Device API key (it_dev_...) issued when the device is paired; "
                        + "required on the edge routes, which accept no user JWT")));

    return openApi;
  }

  /**
   * Completes the error contract of every documented operation, so no controller has to repeat it.
   *
   * <p>For each operation it adds, when the operation does not document them itself:</p>
   * <ul>
   *   <li>{@code 400 VALIDATION_ERROR}, when the operation takes parameters or a body;</li>
   *   <li>{@code 401 UNAUTHORIZED}, unless the route is public (sign-in, sign-up, the Google token
   *       exchange, refresh and logout, and the ticket-authenticated telemetry stream, which document
   *       their own {@code 401} where they answer one);</li>
   *   <li>{@code 429 TOO_MANY_REQUESTS} with a {@code Retry-After} header on the throttled routes;</li>
   *   <li>{@code 503 GEOCODING_UNAVAILABLE} with a {@code Retry-After} header on the route that calls
   *       the geocoding service synchronously;</li>
   *   <li>{@code 500 UNEXPECTED_ERROR}, everywhere.</li>
   * </ul>
   *
   * <p>Every {@code 400}, {@code 401}, {@code 429}, {@code 500} and {@code 503} answer, added here
   * or declared by the controller without a body, is then given the {@code ErrorResponse} schema, the
   * {@code {code, message, details}} body {@code ErrorResponseAssembler} writes. The exception is the
   * {@code 401} of the edge routes, which the device API key chain answers with an empty body. Public
   * routes are also marked as requiring no security scheme, and the responses of each operation are
   * listed in status code order. Endpoint-specific answers ({@code 403}, {@code 404}, {@code 409},
   * {@code 422}) stay explicit on each method.</p>
   *
   * @return the customizer applied by springdoc after the paths are generated
   */
  @Bean
  public OpenApiCustomizer standardErrorResponsesCustomizer() {
    return openApi -> {
      if (openApi.getPaths() == null) {
        return;
      }
      registerErrorSchema(openApi);
      openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap()
          .forEach((method, operation) -> completeErrorResponses(path, method, operation)));
    };
  }

  /**
   * Adds the standard error responses an operation leaves out, and the error body to those it
   * declares without one.
   *
   * @param path      the path template of the operation
   * @param method    the HTTP method of the operation
   * @param operation the operation to complete
   */
  private static void completeErrorResponses(String path, PathItem.HttpMethod method, Operation operation) {
    var responses = operation.getResponses() == null ? new ApiResponses() : operation.getResponses();
    var publicRoute = isPublicRoute(path, method);
    var edgeRoute = usesDeviceApiKey(operation);
    if (publicRoute) {
      // An empty list, unlike an absent one, overrides the document-wide bearer requirement.
      operation.setSecurity(new ArrayList<>());
    }

    if (hasInput(operation)) {
      responses.putIfAbsent("400", new ApiResponse()
          .description("VALIDATION_ERROR - a parameter or the request body is missing, malformed or out of range"));
    }
    if (!publicRoute) {
      responses.putIfAbsent("401", new ApiResponse().description(edgeRoute
          ? "Missing, unknown, revoked or malformed API key; the body is empty"
          : "UNAUTHORIZED - JWT token required or invalid"));
    }
    if (RATE_LIMITED_PATHS.contains(path)) {
      responses.putIfAbsent("429", new ApiResponse()
          .description("TOO_MANY_REQUESTS - the caller exceeded its rate limit; see the Retry-After header"));
    }
    if (GEOCODING_PATHS.contains(path)) {
      responses.putIfAbsent("503", new ApiResponse()
          .description("GEOCODING_UNAVAILABLE - the maps service failed or timed out; see the Retry-After header"));
    }
    responses.putIfAbsent("500", new ApiResponse()
        .description("UNEXPECTED_ERROR - the details carry only a reference to the server log"));

    for (var statusCode : STANDARD_ERROR_STATUS_CODES) {
      var response = responses.get(statusCode);
      if (response == null || response.get$ref() != null) {
        continue;
      }
      if (response.getContent() == null && !(edgeRoute && "401".equals(statusCode))) {
        response.setContent(errorContent());
      }
      if (("429".equals(statusCode) || "503".equals(statusCode))
          && (response.getHeaders() == null || !response.getHeaders().containsKey(RETRY_AFTER_HEADER))) {
        response.addHeaderObject(RETRY_AFTER_HEADER, new Header()
            .description("Seconds to wait before retrying")
            .schema(new IntegerSchema()));
      }
    }

    var ordered = new ApiResponses();
    responses.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> ordered.addApiResponse(entry.getKey(), entry.getValue()));
    ordered.setExtensions(responses.getExtensions());
    operation.setResponses(ordered);
  }

  /**
   * Registers the {@code ErrorResponse} schema, normally already present because controllers
   * reference it, so every reference added by the customizer resolves.
   *
   * @param openApi the document being completed
   */
  private static void registerErrorSchema(OpenAPI openApi) {
    if (openApi.getComponents() == null) {
      openApi.setComponents(new Components());
    }
    var components = openApi.getComponents();
    if (components.getSchemas() == null || !components.getSchemas().containsKey(ERROR_SCHEMA_NAME)) {
      ModelConverters.getInstance().readAll(ErrorResource.class).forEach(components::addSchemas);
    }
  }

  /** @return a fresh JSON content referencing the {@code ErrorResponse} schema */
  private static Content errorContent() {
    return new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
        new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA_NAME)));
  }

  /**
   * Tells whether a route is reachable without a bearer token, mirroring the {@code permitAll}
   * rules of the user security filter chain.
   */
  private static boolean isPublicRoute(String path, PathItem.HttpMethod method) {
    if (path.startsWith(AUTHENTICATION_PATH_PREFIX)) {
      return !AUTHENTICATED_ME_PATH.equals(path);
    }
    return TELEMETRY_STREAM_PATH.equals(path) && method == PathItem.HttpMethod.GET;
  }

  /** Tells whether an operation is an edge route, authenticated by the device API key. */
  private static boolean usesDeviceApiKey(Operation operation) {
    return operation.getSecurity() != null && operation.getSecurity().stream()
        .anyMatch(requirement -> requirement.containsKey(DEVICE_API_KEY_SCHEME));
  }

  /** Tells whether an operation receives anything a request can get wrong. */
  private static boolean hasInput(Operation operation) {
    return operation.getRequestBody() != null
        || (operation.getParameters() != null && !operation.getParameters().isEmpty());
  }
}
