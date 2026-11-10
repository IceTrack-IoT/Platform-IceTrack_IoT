package pe.edu.upc.ice.track.platform.iam.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Throttles the endpoints that verify a credential, answering {@code 429 TOO_MANY_REQUESTS} once
 * a client has failed too often.
 *
 * <p>Covers {@code POST /api/v1/authentication/sign-in/local}, {@code /google/verify} and
 * {@code /google/link}. Every request is attributed to the client address, and a local sign-in
 * also to the submitted username or email, lowercased. After the request is handled, a
 * {@code 401} counts as one failure against each of those keys in {@link SignInAttemptRegistry};
 * a successful response clears the account key, while the address key keeps its count, so a
 * client cannot reset its own limit by signing in to an account it controls between guesses. A
 * request arriving while any of its keys is blocked is answered {@code 429} with a
 * {@code Retry-After} header, without reaching the controller.</p>
 *
 * <p>Only a {@code 401} is a failure. A {@code 403 ROLE_CHANNEL_MISMATCH} means the credentials
 * were right, and a {@code 400} means nothing was verified.</p>
 *
 * <p>The client address is {@link HttpServletRequest#getRemoteAddr()}. Behind a reverse proxy,
 * {@code server.forward-headers-strategy} must be set so that it is the client's address rather
 * than the proxy's.</p>
 *
 * <p>Deliberately not a bean: Spring Boot registers every {@code Filter} bean as a servlet filter
 * too, so it would also run outside the security chain.</p>
 */
@Slf4j
public class SignInRateLimitFilter extends OncePerRequestFilter {

  static final String LOCAL_SIGN_IN_PATH = "/api/v1/authentication/sign-in/local";
  static final Set<String> RATE_LIMITED_PATHS = Set.of(
      LOCAL_SIGN_IN_PATH,
      "/api/v1/authentication/google/verify",
      "/api/v1/authentication/google/link");

  /** Larger sign-in bodies are passed through untouched and only throttled by address. */
  private static final int MAX_INSPECTED_BODY_BYTES = 16 * 1024;
  private static final String ADDRESS_KEY_PREFIX = "address:";
  private static final String ACCOUNT_KEY_PREFIX = "account:";

  private final SignInAttemptRegistry signInAttemptRegistry;
  private final ObjectMapper objectMapper;

  /**
   * Creates the filter.
   *
   * @param signInAttemptRegistry the registry counting failures
   * @param objectMapper          the mapper reading the sign-in body and writing the 429 body
   */
  public SignInRateLimitFilter(SignInAttemptRegistry signInAttemptRegistry, ObjectMapper objectMapper) {
    this.signInAttemptRegistry = signInAttemptRegistry;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !HttpMethod.POST.matches(request.getMethod()) || !RATE_LIMITED_PATHS.contains(pathWithinApplication(request));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var path = pathWithinApplication(request);
    HttpServletRequest forwardedRequest = request;
    String accountKey = null;
    if (LOCAL_SIGN_IN_PATH.equals(path)) {
      var cachedRequest = new CachedBodyHttpServletRequest(request, MAX_INSPECTED_BODY_BYTES);
      forwardedRequest = cachedRequest;
      accountKey = cachedRequest.completeBody()
          .flatMap(this::readUsername)
          .map(username -> ACCOUNT_KEY_PREFIX + username.trim().toLowerCase(Locale.ROOT))
          .orElse(null);
    }

    var keys = new ArrayList<String>(2);
    keys.add(ADDRESS_KEY_PREFIX + request.getRemoteAddr());
    if (accountKey != null) {
      keys.add(accountKey);
    }

    var retryAfter = signInAttemptRegistry.retryAfter(keys);
    if (retryAfter.isPresent()) {
      var seconds = toWholeSeconds(retryAfter.get());
      log.warn("Throttled {} from {} for {} more seconds", path, request.getRemoteAddr(), seconds);
      writeTooManyRequests(response, seconds);
      return;
    }

    filterChain.doFilter(forwardedRequest, response);

    var status = response.getStatus();
    if (status == HttpStatus.UNAUTHORIZED.value()) {
      signInAttemptRegistry.recordFailure(keys);
    } else if (accountKey != null && status >= 200 && status < 300) {
      signInAttemptRegistry.reset(accountKey);
    }
  }

  /**
   * Reads the {@code username} of a sign-in body.
   *
   * @param body the complete request body
   * @return the non-blank username, or empty when the body is not a JSON object carrying one
   */
  private Optional<String> readUsername(byte[] body) {
    try {
      return Optional.ofNullable(objectMapper.readValue(body, UsernameProbe.class))
          .map(UsernameProbe::username)
          .filter(username -> !username.isBlank());
    } catch (RuntimeException exception) {
      // Malformed bodies are rejected by the controller; they are only throttled by address.
      return Optional.empty();
    }
  }

  private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
    var error = ApplicationError.tooManyRequests("Retry after %d seconds".formatted(retryAfterSeconds));
    var errorResponse = ErrorResponseAssembler.toErrorResponseFromApplicationError(error);
    response.setStatus(errorResponse.getStatusCode().value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getOutputStream(), errorResponse.getBody());
  }

  private static long toWholeSeconds(Duration duration) {
    return Math.max(1L, (duration.toMillis() + 999L) / 1000L);
  }

  private static String pathWithinApplication(HttpServletRequest request) {
    var uri = request.getRequestURI();
    var contextPath = request.getContextPath();
    return contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
        ? uri.substring(contextPath.length())
        : uri;
  }

  /**
   * The only field of a sign-in body this filter reads.
   *
   * @param username the submitted username or email
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record UsernameProbe(String username) {
  }
}
