package pe.edu.upc.ice.track.platform.iam.infrastructure.authorization.sfs.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import pe.edu.upc.ice.track.platform.iam.infrastructure.tokens.jwt.BearerTokenService;

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
 * Spring Boot's resource-server auto-configuration stays inactive and this filter chain remains
 * the single authority on request authorization.
 * </p>
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {

  private final UserDetailsService userDetailsService;

  private final BearerTokenService tokenService;

  private final BCryptHashingService hashingService;

  private final AuthenticationEntryPoint unauthorizedRequestHandler;

  /**
   * This method creates the Bearer Authorization Request Filter.
   * @return The Bearer Authorization Request Filter
   * @see BearerAuthorizationRequestFilter
   */
  @Bean
  public BearerAuthorizationRequestFilter authorizationRequestFilter() {
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
      cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
      cors.setAllowedHeaders(List.of("*"));
      return cors;
    }));
    http.csrf(AbstractHttpConfigurer::disable)
        .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(unauthorizedRequestHandler))
        .sessionManagement( customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorizeRequests -> authorizeRequests
            .requestMatchers(
                // Sign-in, sign-up and the Google token exchange must be reachable anonymously:
                // they are what mints the bearer token every other endpoint requires.
                "/api/v1/authentication/**",
                // Authorization rules also apply to the ERROR dispatch, so the container error
                // endpoint has to stay reachable for sendError() responses to be rendered.
                "/error",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/swagger-resources/**",
                "/webjars/**").permitAll()
            .anyRequest().authenticated());
    http.authenticationProvider(authenticationProvider());
    http.addFilterBefore(authorizationRequestFilter(), UsernamePasswordAuthenticationFilter.class);
    return http.build();

  }

  /**
   * This is the constructor of the class.
   * @param userDetailsService The user details service
   * @param tokenService The token service
   * @param hashingService The hashing service
   * @param authenticationEntryPoint The authentication entry point
   */
  public WebSecurityConfiguration(@Qualifier("defaultUserDetailsService") UserDetailsService userDetailsService, BearerTokenService tokenService, BCryptHashingService hashingService, AuthenticationEntryPoint authenticationEntryPoint) {
    this.userDetailsService = userDetailsService;
    this.tokenService = tokenService;
    this.hashingService = hashingService;
    this.unauthorizedRequestHandler = authenticationEntryPoint;
  }
}
