package pe.edu.upc.ice.track.platform.device.infrastructure.authorization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import pe.edu.upc.ice.track.platform.device.application.queryservices.DeviceQueryService;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.RoleAuthorities;

/**
 * Security filter chain of the edge routes: the endpoints IoT boards call.
 *
 * <p>Ordered before the user chain, which matches every other request, so these routes never reach
 * the JWT filter. They authenticate by device API key alone ({@link ApiKeyAuthenticationFilter})
 * and require {@link RoleAuthorities#ROLE_DEVICE}, which no user account holds: a user JWT is
 * neither needed nor accepted here, and a missing, unknown, revoked or malformed key answers
 * 401.</p>
 *
 * <p>Only the ingestion half of the telemetry API is an edge route. Reading history
 * ({@code GET /api/v1/telemetry/equipment/{id}}) stays on the user chain.</p>
 */
@Configuration
public class EdgeSecurityConfiguration {

  private static final RequestMatcher EDGE_ROUTES = new OrRequestMatcher(
      PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/telemetry"),
      PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/telemetry/events"),
      PathPatternRequestMatcher.withDefaults().matcher("/api/v1/devices/me/**"));

  @Bean
  @Order(1)
  public SecurityFilterChain edgeFilterChain(HttpSecurity http, DeviceQueryService deviceQueryService)
      throws Exception {
    http.securityMatcher(EDGE_ROUTES)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().hasAuthority(RoleAuthorities.ROLE_DEVICE))
        // Instantiated here rather than exposed as a bean, so Spring Boot does not also register
        // it as a servlet filter outside the security chain.
        .addFilterBefore(new ApiKeyAuthenticationFilter(deviceQueryService),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
