package pe.edu.upc.ice.track.platform.device.infrastructure.authorization;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.upc.ice.track.platform.device.application.queryservices.DeviceQueryService;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.AuthenticatedDevice;

import java.io.IOException;

/**
 * Authenticates edge requests by the device API key carried in the {@value #API_KEY_HEADER}
 * header.
 *
 * <p>The key alone identifies the board: it is digested and looked up through the unique index on
 * the stored digest, and only a {@code PAIRED} board is accepted. On success a
 * {@link DeviceAuthenticationToken} holding the server-side device and equipment identifiers is
 * placed in the security context. On failure the request continues unauthenticated, the attempt
 * is logged, and the edge filter chain answers 401 because its routes require the device
 * authority.</p>
 *
 * <p>Neither the key nor any part of it is ever logged. This filter is built by
 * {@link EdgeSecurityConfiguration} and deliberately not exposed as a bean, so Spring Boot does not
 * also register it as a servlet filter running outside the security chain.</p>
 */
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  public static final String API_KEY_HEADER = "X-API-Key";

  private final DeviceQueryService deviceQueryService;
  private final SecurityContextHolderStrategy securityContextHolderStrategy =
      SecurityContextHolder.getContextHolderStrategy();

  public ApiKeyAuthenticationFilter(DeviceQueryService deviceQueryService) {
    this.deviceQueryService = deviceQueryService;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    var apiKey = request.getHeader(API_KEY_HEADER);
    if (apiKey == null || apiKey.isBlank()) {
      log.warn("Rejected edge request {} {} from {}: no API key",
          request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
    } else {
      deviceQueryService.authenticateByApiKey(apiKey.trim()).ifPresentOrElse(
          device -> {
            var context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(new DeviceAuthenticationToken(
                new AuthenticatedDevice(device.deviceId(), device.equipmentId())));
            securityContextHolderStrategy.setContext(context);
          },
          () -> log.warn("Rejected edge request {} {} from {}: unknown, revoked or malformed API key",
              request.getMethod(), request.getRequestURI(), request.getRemoteAddr()));
    }
    filterChain.doFilter(request, response);
  }
}
