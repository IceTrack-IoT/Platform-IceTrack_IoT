package pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.function.Function;

/**
 * Assembler for conditional reads: answers a representation together with its {@code ETag}, or
 * {@code 304 Not Modified} with an empty body when the caller already holds that representation.
 *
 * <p>The entity tag is a digest of the representation itself, so it changes exactly when what the
 * client would receive changes, and never because of a write that leaves the representation alone.
 * That is what makes it usable for directory-style reads whose rows are touched by unrelated
 * writes, such as an equipment row updated by every telemetry batch.</p>
 *
 * <p>The digest is taken over the text form of the representation, so the representation must be
 * made of types whose {@code toString} is value-based and deterministic: records, lists, enums,
 * strings and boxed primitives. Both answers carry {@code Cache-Control: private, no-cache}: a
 * browser may keep the body, for that user only, and must revalidate it on every use.</p>
 */
@NullMarked
public final class ConditionalResponseAssembler {

  /** Number of hexadecimal characters of the digest kept in the entity tag: 128 bits. */
  private static final int ENTITY_TAG_LENGTH = 32;

  private ConditionalResponseAssembler() {
  }

  /**
   * Answers a representation conditionally.
   *
   * @param representation the resource, or list of resources, the endpoint answers
   * @param ifNoneMatch    the {@code If-None-Match} header of the request, may be {@code null}
   * @return 200 with the representation and its {@code ETag}, or 304 with the {@code ETag} alone
   *         when the header names it
   */
  public static ResponseEntity<?> toConditionalResponse(Object representation, @Nullable String ifNoneMatch) {
    var entityTag = toEntityTag(representation);
    if (matchesAny(ifNoneMatch, entityTag)) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
          .eTag(entityTag)
          .cacheControl(revalidateAlways())
          .varyBy(HttpHeaders.AUTHORIZATION)
          .build();
    }
    return ResponseEntity.ok()
        .eTag(entityTag)
        .cacheControl(revalidateAlways())
        .varyBy(HttpHeaders.AUTHORIZATION)
        .body(representation);
  }

  /**
   * Answers the outcome of a query conditionally: a success through
   * {@link #toConditionalResponse(Object, String)}, a failure through {@link ErrorResponseAssembler}.
   *
   * @param result                   the application result
   * @param successResourceAssembler function that maps the success value to its representation
   * @param ifNoneMatch              the {@code If-None-Match} header of the request, may be {@code null}
   * @param <T>                      success value type
   * @param <R>                      representation type
   * @return the conditional response of a success, or the error response of a failure
   */
  public static <T, R> ResponseEntity<?> toConditionalResponseFromResult(
      Result<T, ApplicationError> result,
      Function<T, R> successResourceAssembler,
      @Nullable String ifNoneMatch) {
    return switch (result) {
      case Result.Success<T, ApplicationError> success ->
          toConditionalResponse(successResourceAssembler.apply(success.value()), ifNoneMatch);
      case Result.Failure<T, ApplicationError> failure ->
          ErrorResponseAssembler.toErrorResponseFromApplicationError(failure.error());
    };
  }

  /**
   * Computes the strong entity tag of a representation.
   *
   * @param representation the representation
   * @return the quoted entity tag
   */
  public static String toEntityTag(Object representation) {
    try {
      var digest = MessageDigest.getInstance("SHA-256")
          .digest(String.valueOf(representation).getBytes(StandardCharsets.UTF_8));
      return "\"" + HexFormat.of().formatHex(digest).substring(0, ENTITY_TAG_LENGTH) + "\"";
    } catch (NoSuchAlgorithmException exception) {
      // Every Java runtime is required to provide SHA-256.
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  /**
   * Tells whether an {@code If-None-Match} header names an entity tag. Accepts a list of tags, weak
   * tags ({@code W/"..."}), the bare value and {@code *}.
   *
   * @param ifNoneMatch the header value, may be {@code null}
   * @param entityTag   the current entity tag, quoted
   * @return {@code true} when the caller already holds the current representation
   */
  public static boolean matchesAny(@Nullable String ifNoneMatch, String entityTag) {
    if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
      return false;
    }
    var bare = entityTag.substring(1, entityTag.length() - 1);
    return Arrays.stream(ifNoneMatch.split(","))
        .map(String::trim)
        .map(tag -> tag.startsWith("W/") ? tag.substring(2) : tag)
        .anyMatch(tag -> tag.equals("*") || tag.equals(entityTag) || tag.equals(bare));
  }

  private static CacheControl revalidateAlways() {
    return CacheControl.noCache().cachePrivate();
  }
}
