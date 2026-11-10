package pe.edu.upc.ice.track.platform.iam.infrastructure.authorization.sfs.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import pe.edu.upc.ice.track.platform.iam.infrastructure.authorization.sfs.pipeline.BearerAuthorizationRequestFilter;
import pe.edu.upc.ice.track.platform.iam.infrastructure.hashing.bcrypt.BCryptHashingService;
import pe.edu.upc.ice.track.platform.iam.infrastructure.security.SignInAttemptRegistry;
import pe.edu.upc.ice.track.platform.iam.infrastructure.security.SignInRateLimitFilter;
import pe.edu.upc.ice.track.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Web Security Configuration.
 * <p>
 * This class is responsible for configuring the web security.
 * It enables the method security and configures the security filter chain.
 * It includes the authentication manager, the authentication provider, the password encoder and the authentication entry point.
 * </p>
 * <p>
 * API authorization keeps relying on the platform's own bearer token, validated by
 * {@link BearerAuthorizationRequestFilter} through the {@code tokens.jwt} HMAC service. The
 * {@code spring-boot-starter-oauth2-resource-server} dependency is present only to supply the
 * {@code NimbusJwtDecoder} that validates incoming <em>Google</em> id_tokens at the token
 * exchange endpoint; no {@code spring.security.oauth2.resourceserver.*} property is declared, so
 * Spring Boot's resource-server autoconfiguration stays inactive and this filter chain remains
 * the single authority on user request authorization.
 * </p>
 * <p>
 * This chain has no security matcher and is therefore evaluated last. The edge routes the IoT
 * boards call ({@code POST /api/v1/telemetry}, {@code POST /api/v1/telemetry/events} and
 * {@code /api/v1/devices/me/**}) are claimed first by the device API key chain of the Device
 * Management context, so they never reach the bearer token filter.
 * </p>
 * <p>
 * The endpoints that verify a credential ({@code /sign-in/local}, {@code /google/verify} and
 * {@code /google/link}) are throttled by {@link SignInRateLimitFilter}, which answers
 * {@code 429 TOO_MANY_REQUESTS} once a client address or an account has failed too often.
 * </p>
 * <p>
 * {@code GET /api/v1/telemetry/stream} is reachable without a bearer token: browsers open it with
 * {@code EventSource}, which cannot send one, and it is authenticated by the single-use stream ticket
 * its query string carries, issued to an owner by {@code POST /api/v1/telemetry/stream-ticket}.
 * </p>
 * <p>
 * A request rejected for lack of a valid bearer token is answered {@code 401} with the standard
 * {@code {code, message, details}} body, written by {@code UnauthorizedRequestHandlerEntryPoint}.
 * </p>
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {

  /** Server-Sent Events route of the telemetry stream, authenticated by a stream ticket instead of a JWT. */
  static final String TELEMETRY_STREAM_PATH = "/api/v1/telemetry/stream";

  private final UserDetailsService userDetailsService;

  private final BearerTokenService tokenService;

  private final BCryptHashingService hashingService;

  private final AuthenticationEntryPoint unauthorizedRequestHandler;

  private final SignInAttemptRegistry signInAttemptRegistry;

  private final ObjectMapper objectMapper;

  /**
   * Creates the filter throttling failed sign-in attempts.
   *
   * <p>Deliberately not a {@code @Bean}, for the same reason as
   * {@link #authorizationRequestFilter()}.</p>
   *
   * @return The sign-in rate limit filter
   * @see SignInRateLimitFilter
   */
  private SignInRateLimitFilter signInRateLimitFilter() {
    return new SignInRateLimitFilter(signInAttemptRegistry, objectMapper);
  }

  /**
   * Creates the Bearer Authorization Request Filter.
   *
   * <p>Deliberately not a {@code @Bean}: Spring Boot registers every {@code Filter} bean as a
   * servlet filter too, so a bean would also run outside the security chain, edge routes
   * included.</p>
   *
   * @return The Bearer Authorization Request Filter
   * @see BearerAuthorizationRequestFilter
   */
  private BearerAuthorizationRequestFilter authorizationRequestFilter() {
    return new BearerAuthorizationRequestFilter(tokenService, userDetailsService);
  }

  /**
   * This method creates the authentication manager.
   * @param authenticationConfiguration The {@link AuthenticationConfiguration} object with the authentication configuration
   * @return The {@link AuthenticationManager} instance from the authentication configuration
   *
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  /**
   * This method creates the authentication provider.
   * @return The {@link DaoAuthenticationProvider} authentication provider with the user details service and the password encoder
   */
  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    var authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
    authenticationProvider.setPasswordEncoder(hashingService);
    return authenticationProvider;
  }

  /**
   * This method creates the password encoder.
   * @return The {@link PasswordEncoder} instance with the hashing service
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return hashingService;
  }

  /**
   * This method creates the security filter chain.
   * It also configures the http security.
   *
   * @param http The {@link HttpSecurity} object to configure with the security filter chain
   * @return The {@link SecurityFilterChain} instance with the application http security configuration
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(configurer -> configurer.configurationSource(_ -> {
      var cors = new CorsConfiguration();
      cors.setAllowedOrigins(List.of("*"));
      cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
      cors.setAllowedHeaders(List.of("*"));
      // Without this a browser hides these headers from the web application: ETag is what a
      // conditional read sends back in If-None-Match, Retry-After is how long to wait after a 429
      // or a 503, and X-Telemetry-Bucket is the bucket width an aggregated telemetry history used.
      cors.setExposedHeaders(List.of("ETag", "Retry-After", "X-Telemetry-Bucket"));
      return cors;
    }));
    http.csrf(AbstractHttpConfigurer::disable)
        .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(unauthorizedRequestHandler))
        .sessionManagement( customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorizeRequests -> authorizeRequests
            // Rules are evaluated in order: the current-user endpoint must be matched before the
            // anonymous authentication wildcard below, or that wildcard would expose it.
            .requestMatchers("/api/v1/authentication/me").authenticated()
            .requestMatchers(
                // Sign-in, sign-up and the Google token exchange must be reachable anonymously:
                // they are what mints the bearer token every other endpoint requires. So must
                // refresh-token and logout: the refresh token they receive is their credential,
                // and the access token may already have expired when they are called.
                "/api/v1/authentication/**",
                // Authorization rules also apply to the ERROR dispatch, so the container error
                // endpoint has to stay reachable for sendError() responses to be rendered.
                "/error",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/swagger-resources/**",
                "/webjars/**").permitAll()
            // The telemetry stream is opened by a browser EventSource, which cannot send a bearer
            // token: its single-use stream ticket, checked by the controller, is its credential.
            .requestMatchers(HttpMethod.GET, TELEMETRY_STREAM_PATH).permitAll()
            .anyRequest().authenticated());
    http.authenticationProvider(authenticationProvider());
    http.addFilterBefore(signInRateLimitFilter(), UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(authorizationRequestFilter(), UsernamePasswordAuthenticationFilter.class);
    return http.build();

  }

  /**
   * This is the constructor of the class.
   * @param userDetailsService The user details service
   * @param tokenService The token service
   * @param hashingService The hashing service
   * @param authenticationEntryPoint The authentication entry point
   * @param signInAttemptRegistry The registry counting failed sign-in attempts
   * @param objectMapper The JSON mapper the sign-in rate limit filter reads and writes bodies with
   */
  public WebSecurityConfiguration(
      @Qualifier("defaultUserDetailsService") UserDetailsService userDetailsService,
      BearerTokenService tokenService,
      BCryptHashingService hashingService,
      AuthenticationEntryPoint authenticationEntryPoint,
      SignInAttemptRegistry signInAttemptRegistry,
      ObjectMapper objectMapper) {
    this.userDetailsService = userDetailsService;
    this.tokenService = tokenService;
    this.hashingService = hashingService;
    this.unauthorizedRequestHandler = authenticationEntryPoint;
    this.signInAttemptRegistry = signInAttemptRegistry;
    this.objectMapper = objectMapper;
  }
}
