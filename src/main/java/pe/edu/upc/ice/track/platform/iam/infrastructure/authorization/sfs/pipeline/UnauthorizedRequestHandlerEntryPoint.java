package pe.edu.upc.ice.track.platform.iam.infrastructure.authorization.sfs.pipeline;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Unauthorized Request Handler.
 * <p>
 * Called by Spring Security when a request of the user filter chain requires authentication and
 * carries no valid bearer token. It answers {@code 401} with the standard error body,
 * {@code {"code": "UNAUTHORIZED", "message": ..., "details": ...}}, the same shape every other error
 * of the API uses and the one the OpenAPI document declares for {@code 401}.
 * </p>
 * @see AuthenticationEntryPoint
 */
@Component
@Slf4j
public class UnauthorizedRequestHandlerEntryPoint implements AuthenticationEntryPoint {

  private static final String REASON = "A valid bearer token is required";

  private final ObjectMapper objectMapper;

  /**
   * Constructor
   *
   * @param objectMapper the JSON mapper the error body is written with
   */
  public UnauthorizedRequestHandlerEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * This method is called by the Spring Security framework when an unauthorized request is detected.
   * @param request The request that caused the exception
   * @param response The response that will be sent to the client
   * @param authenticationException The exception that caused the invocation
   */
  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
                       AuthenticationException authenticationException) throws IOException {
    log.error("Unauthorized request: {}", authenticationException.getMessage());
    if (response.isCommitted()) {
      return;
    }
    var errorResponse = ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.unauthorized(REASON));
    response.setStatus(errorResponse.getStatusCode().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getOutputStream(), errorResponse.getBody());
  }
}
