package pe.edu.upc.ice.track.platform.iam.infrastructure.authorization.sfs.pipeline;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.upc.ice.track.platform.iam.infrastructure.authorization.sfs.model.UsernamePasswordAuthenticationTokenBuilder;
import pe.edu.upc.ice.track.platform.iam.infrastructure.tokens.jwt.BearerTokenService;

import java.io.IOException;

/**
 * Bearer Authorization Request Filter.
 * <p>
 * This class is responsible for filtering requests and setting the user authentication.
 * It extends the OncePerRequestFilter class.
 * </p>
 * <p>
 * No token material is ever logged, not even a prefix: a bearer token is a credential, and log
 * files are read by far more people and systems than the API is. A request without a bearer token
 * simply continues unauthenticated, and the security rules decide whether that is acceptable.
 * </p>
 * @see OncePerRequestFilter
 */
@Slf4j
public class BearerAuthorizationRequestFilter extends OncePerRequestFilter {

  private final BearerTokenService tokenService;


  @Qualifier("defaultUserDetailsService")
  private final UserDetailsService userDetailsService;

  public BearerAuthorizationRequestFilter(BearerTokenService tokenService, UserDetailsService userDetailsService) {
    this.tokenService = tokenService;
    this.userDetailsService = userDetailsService;
  }

  /**
   * This method is responsible for filtering requests and setting the user authentication.
   * @param request The request object.
   * @param response The response object.
   * @param filterChain The filter chain object.
   */
  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
    String token = tokenService.getBearerTokenFrom(request);
    if (token != null) {
      try {
        if (tokenService.validateToken(token)) {
          String username = tokenService.getUsernameFromToken(token);
          var userDetails = userDetailsService.loadUserByUsername(username);
          SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationTokenBuilder.build(userDetails, request));
        } else {
          log.debug("Bearer token rejected for {} {}", request.getMethod(), request.getRequestURI());
        }
      } catch (RuntimeException e) {
        // The exception type only: messages from the user lookup or the token parser can echo input.
        log.warn("Cannot set user authentication: {}", e.getClass().getSimpleName());
      }
    }
    filterChain.doFilter(request, response);
  }
}
