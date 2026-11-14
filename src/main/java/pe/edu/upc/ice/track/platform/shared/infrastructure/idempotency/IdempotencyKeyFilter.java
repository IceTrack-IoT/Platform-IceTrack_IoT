package pe.edu.upc.ice.track.platform.shared.infrastructure.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Makes the creating endpoints safe to retry with an {@code Idempotency-Key} request header.
 *
 * <p>A client generates a UUID once per submission and sends it again only when it retries that
 * submission, typically after a network failure left it unsure whether the first attempt went through.
 * For a key sent by the same caller to the same endpoint within 24 hours:</p>
 * <ul>
 *   <li>the first request runs normally, and a successful answer (any 2xx) is stored;</li>
 *   <li>a retry with the same body is answered with the stored status and body, plus the response header
 *       {@code Idempotent-Replayed: true}, without running again;</li>
 *   <li>a request with the same key and another body is refused with {@code 409 IDEMPOTENCY_KEY_CONFLICT};</li>
 *   <li>a request arriving while the first one still runs is refused with
 *       {@code 409 IDEMPOTENCY_KEY_CONFLICT}, {@code details: "in progress"} and {@code Retry-After: 1}.</li>
 * </ul>
 * <p>A first request that fails (any status outside 2xx, or an exception) stores nothing and frees the
 * key, so the corrected request can be sent again with it. A key that is not a UUID is refused with
 * {@code 400 VALIDATION_ERROR}. Without the header nothing changes.</p>
 *
 * <p>Secrets are never stored for a replay: the answer of {@code POST /api/v1/devices} carries the
 * device's plaintext API key once, so its {@code api_key} property is removed before storing, and a
 * replay answers the device without it.</p>
 *
 * <p>Registered as a servlet filter after the Spring Security filter chain, so the caller is already
 * authenticated when it runs; it applies to the guarded {@code POST} endpoints only. The guarded paths
 * are listed here as plain strings, which keeps the shared kernel free of any bounded context type.</p>
 */
@Component
@Slf4j
public class IdempotencyKeyFilter extends OncePerRequestFilter {

  /** Request header carrying the key. */
  public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

  /** Response header marking a replayed answer. */
  public static final String REPLAYED_HEADER = "Idempotent-Replayed";

  /** Error code of a key reused with another body, or while its first request still runs. */
  public static final String CONFLICT_CODE = "IDEMPOTENCY_KEY_CONFLICT";

  /** The guarded endpoints, by path, each with the top-level response properties never stored. */
  static final Map<String, Set<String>> GUARDED_ENDPOINTS = Map.of(
      "/api/v1/service-requests", Set.of(),
      "/api/v1/reviews", Set.of(),
      "/api/v1/devices", Set.of("api_key"),
      "/api/v1/assets/equipments", Set.of(),
      "/api/v1/assets/sites", Set.of(),
      "/api/v1/reports", Set.of());

  private static final Pattern UUID_FORMAT =
      Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  private static final int CLAIM_ATTEMPTS = 2;

  private final IdempotencyRecordStore store;
  private final ObjectMapper objectMapper;

  public IdempotencyKeyFilter(IdempotencyRecordStore store, ObjectMapper objectMapper) {
    this.store = store;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !HttpMethod.POST.matches(request.getMethod())
        || !GUARDED_ENDPOINTS.containsKey(pathOf(request))
        || request.getHeader(IDEMPOTENCY_KEY_HEADER) == null;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var rawKey = request.getHeader(IDEMPOTENCY_KEY_HEADER).trim();
    if (!UUID_FORMAT.matcher(rawKey).matches()) {
      writeError(response, ApplicationError.validationError(IDEMPOTENCY_KEY_HEADER,
          "Idempotency-Key must be a UUID, such as 3f2b8c1e-6d4a-4f0e-9b7a-2c5d8e1f0a3b"), null);
      return;
    }
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      chain.doFilter(request, response);
      return;
    }

    var path = pathOf(request);
    var caller = authentication.getName();
    var endpoint = "POST " + path;
    var key = rawKey.toLowerCase(Locale.ROOT);
    var cachedRequest = new CachedBodyHttpServletRequest(request);
    var requestHash = sha256(cachedRequest.body());

    var claim = claimOrAnswer(caller, endpoint, key, requestHash, response);
    if (claim.isEmpty()) {
      return;
    }

    var responseWrapper = new ContentCachingResponseWrapper(response);
    try {
      chain.doFilter(cachedRequest, responseWrapper);
    } catch (IOException | ServletException | RuntimeException ex) {
      releaseQuietly(claim.get());
      throw ex;
    }
    record(claim.get(), responseWrapper, GUARDED_ENDPOINTS.get(path));
    responseWrapper.copyBodyToResponse();
  }

  /**
   * Claims the key for this request, or answers the request from what the key already holds.
   *
   * @return the record of the claim, or empty when the response has been written already
   */
  private Optional<Long> claimOrAnswer(
      String caller, String endpoint, String key, String requestHash, HttpServletResponse response)
      throws IOException {
    for (var attempt = 0; attempt < CLAIM_ATTEMPTS; attempt++) {
      try {
        return Optional.of(store.claim(caller, endpoint, key, requestHash));
      } catch (DataIntegrityViolationException alreadyClaimed) {
        var existing = store.find(caller, endpoint, key);
        if (existing.isEmpty()) {
          // Released by a failed first request in the meantime: claim it again.
          continue;
        }
        var record = existing.get();
        if (record.getExpiresAt().isBefore(Instant.now())) {
          store.discard(record.getId());
          continue;
        }
        if (!record.getRequestHash().equals(requestHash)) {
          writeError(response, conflict("The key was already used with a different request body"), null);
          return Optional.empty();
        }
        if (!record.isCompleted()) {
          writeError(response, conflict("in progress"), "1");
          return Optional.empty();
        }
        replay(record.getResponseStatus(), record.getResponseContentType(), record.getResponseBody(), response);
        return Optional.empty();
      }
    }
    writeError(response, conflict("in progress"), "1");
    return Optional.empty();
  }

  /**
   * Stores a successful answer, without its secrets, or frees the key of a failed request.
   */
  private void record(Long recordId, ContentCachingResponseWrapper responseWrapper, Set<String> secrets) {
    var status = responseWrapper.getStatus();
    if (status < 200 || status >= 300) {
      releaseQuietly(recordId);
      return;
    }
    var body = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
    var storable = withoutSecrets(body, secrets);
    if (storable.isEmpty()) {
      // The secrets could not be removed: storing nothing is the only safe choice.
      releaseQuietly(recordId);
      return;
    }
    try {
      store.complete(recordId, status, responseWrapper.getContentType(), storable.get());
    } catch (RuntimeException ex) {
      log.warn("The answer to idempotency record {} could not be stored; the key is freed", recordId, ex);
      releaseQuietly(recordId);
    }
  }

  /**
   * Removes the top-level secret properties of a JSON body.
   *
   * @return the body to store, or empty when it has secrets to remove but is not a JSON object
   */
  private Optional<String> withoutSecrets(String body, Set<String> secrets) {
    if (secrets == null || secrets.isEmpty()) {
      return Optional.of(body);
    }
    try {
      if (!(objectMapper.readTree(body) instanceof ObjectNode json)) {
        return Optional.empty();
      }
      secrets.forEach(json::remove);
      return Optional.of(objectMapper.writeValueAsString(json));
    } catch (JacksonException ex) {
      return Optional.empty();
    }
  }

  private static void replay(Integer status, String contentType, String body, HttpServletResponse response)
      throws IOException {
    response.setStatus(status == null ? HttpServletResponse.SC_OK : status);
    response.setHeader(REPLAYED_HEADER, "true");
    if (contentType != null) {
      response.setContentType(contentType);
    }
    if (body != null && !body.isEmpty()) {
      var bytes = body.getBytes(StandardCharsets.UTF_8);
      response.setContentLength(bytes.length);
      response.getOutputStream().write(bytes);
    }
  }

  private void writeError(HttpServletResponse response, ApplicationError error, String retryAfter) throws IOException {
    var entity = ErrorResponseAssembler.toErrorResponseFromApplicationError(error);
    response.setStatus(entity.getStatusCode().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    if (retryAfter != null) {
      response.setHeader("Retry-After", retryAfter);
    }
    response.getOutputStream().write(objectMapper.writeValueAsBytes(entity.getBody()));
  }

  private static ApplicationError conflict(String details) {
    return new ApplicationError(CONFLICT_CODE, "Idempotency key conflict", details);
  }

  private void releaseQuietly(Long recordId) {
    try {
      store.release(recordId);
    } catch (RuntimeException ex) {
      log.warn("Idempotency record {} could not be released; it expires on its own", recordId, ex);
    }
  }

  private static String pathOf(HttpServletRequest request) {
    var uri = request.getRequestURI();
    var contextPath = request.getContextPath();
    var path = contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
        ? uri.substring(contextPath.length())
        : uri;
    return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
  }

  private static String sha256(byte[] body) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
