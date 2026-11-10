package pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation;

/**
 * Outbound port limiting how often one caller may validate addresses.
 */
public interface ValidationRateLimiterPort {

  /**
   * Records one attempt for the key, when the limit allows it.
   *
   * @param key the caller being limited, such as a username; required
   * @return {@code true} when the attempt is allowed, {@code false} when the caller has used up its
   *         quota for the current window
   */
  boolean tryAcquire(String key);
}
