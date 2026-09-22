package pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.hashing;

/**
 * Outbound port for password hashing operations required by the IAM application layer.
 */
public interface HashingService {
  /**
   * Encodes a raw password for persistence.
   *
   * @param rawPassword raw password value
   * @return encoded password representation
   */
  String encode(CharSequence rawPassword);

  /**
   * Verifies whether a raw password matches an encoded password.
   *
   * @param rawPassword raw password value
   * @param encodedPassword encoded password representation
   * @return {@code true} when both values match; otherwise {@code false}
   */
  boolean matches(CharSequence rawPassword, String encodedPassword);
}
