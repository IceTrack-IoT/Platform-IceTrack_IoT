package pe.edu.upc.ice.track.platform.device.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.ice.track.platform.device.application.queryservices.EdgeConfigurationQueryService;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetEdgeConfigurationQuery;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.EdgeConfigurationResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.transform.EdgeConfigurationResourceFromResultAssembler;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.AuthenticatedDevice;
import pe.edu.upc.ice.track.platform.shared.infrastructure.documentation.openapi.configuration.OpenApiConfiguration;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;

import java.util.Arrays;

/**
 * REST controller through which an edge gateway polls the configuration of its unit.
 *
 * <p>An edge route: authenticated by the device API key on the edge filter chain, never by a user
 * JWT. The unit is the one the key is paired with; the request names none.</p>
 *
 * <p>The configuration version is the strong ETag of the response. A gateway that sends the version
 * it already holds in {@code If-None-Match} receives {@code 304 Not Modified} with no body, so a
 * poll on every sync cycle costs almost nothing while nothing changed.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/devices/me", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Edge configuration", description = "Configuration polling endpoint for edge gateways")
public class EdgeConfigurationController {

  private final EdgeConfigurationQueryService edgeConfigurationQueryService;

  public EdgeConfigurationController(EdgeConfigurationQueryService edgeConfigurationQueryService) {
    this.edgeConfigurationQueryService = edgeConfigurationQueryService;
  }

  /**
   * Returns the configuration of the authenticated device's unit, or 304 when the gateway already
   * holds it.
   *
   * @param ifNoneMatch the ETag of the configuration the gateway holds, may be {@code null}
   * @param device      the device authenticated by its API key
   * @return 200 with the configuration and its ETag, 304 when unchanged, or 404 when the unit no
   *         longer exists
   */
  @GetMapping("/config")
  @Operation(
      summary = "Poll the edge configuration",
      description = "Returns what the gateway enforces locally for its unit: thresholds, alert policy and active "
          + "technician grants (SHA-256 digests only). The ETag header carries config_version; sending it back "
          + "in If-None-Match answers 304 with an empty body while nothing changed. The version moves whenever "
          + "the threshold, the alert policy or a grant of the unit changes. Authenticated by the X-API-Key "
          + "header only.",
      security = @SecurityRequirement(name = OpenApiConfiguration.DEVICE_API_KEY_SCHEME))
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Configuration changed since the version in If-None-Match",
          content = @Content(schema = @Schema(implementation = EdgeConfigurationResource.class))),
      @ApiResponse(responseCode = "304", description = "Configuration unchanged; empty body"),
      @ApiResponse(responseCode = "401", description = "Missing, unknown, revoked or malformed API key"),
      @ApiResponse(responseCode = "404", description = "EQUIPMENT_NOT_FOUND - the paired unit no longer exists",
          content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  })
  public ResponseEntity<?> getConfiguration(
      @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false)
      @Parameter(description = "ETag of the configuration the gateway holds, such as \"7\"", example = "\"7\"")
      String ifNoneMatch,
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedDevice device) {
    if (device == null) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.unauthorized("A device API key is required"));
    }
    var configuration = edgeConfigurationQueryService.handle(new GetEdgeConfigurationQuery(device.equipmentId()));
    if (configuration.isEmpty()) {
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(
          ApplicationError.notFound("Equipment", String.valueOf(device.equipmentId())));
    }
    var etag = "\"" + configuration.get().configVersion() + "\"";
    if (matchesAny(ifNoneMatch, etag)) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
    }
    return ResponseEntity.ok()
        .eTag(etag)
        .body(EdgeConfigurationResourceFromResultAssembler.toResourceFromResult(configuration.get()));
  }

  /**
   * Tells whether an {@code If-None-Match} header names the current ETag. Accepts a list of tags,
   * weak tags ({@code W/"7"}), the bare version and {@code *}.
   *
   * @param ifNoneMatch the header value, may be {@code null}
   * @param etag        the current strong ETag, quoted
   * @return {@code true} when the gateway already holds the current configuration
   */
  static boolean matchesAny(String ifNoneMatch, String etag) {
    if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
      return false;
    }
    var bare = etag.substring(1, etag.length() - 1);
    return Arrays.stream(ifNoneMatch.split(","))
        .map(String::trim)
        .map(tag -> tag.startsWith("W/") ? tag.substring(2) : tag)
        .anyMatch(tag -> tag.equals("*") || tag.equals(etag) || tag.equals(bare));
  }
}
