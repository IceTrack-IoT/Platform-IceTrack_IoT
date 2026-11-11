package pe.edu.upc.ice.track.platform.shared.infrastructure.idempotency;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.shared.infrastructure.idempotency.persistence.IdempotencyRecordPersistenceEntity;
import pe.edu.upc.ice.track.platform.shared.infrastructure.idempotency.persistence.IdempotencyRecordPersistenceRepository;
import pe.edu.upc.ice.track.platform.shared.infrastructure.idempotency.persistence.IdempotencyRecordPersistenceSpecifications;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Stores the idempotency records the {@link IdempotencyKeyFilter} relies on.
 *
 * <p>Every method runs in a transaction of its own, separate from the request it guards: a claim is
 * committed before the request runs, so a concurrent retry with the same key finds it, and the stored
 * answer is committed after the request's own transaction already has.</p>
 */
@Component
public class IdempotencyRecordStore {

  /** How long a key is remembered. */
  public static final Duration RETENTION = Duration.ofHours(24);

  private final IdempotencyRecordPersistenceRepository repository;

  public IdempotencyRecordStore(IdempotencyRecordPersistenceRepository repository) {
    this.repository = repository;
  }

  /**
   * Finds the record of a key.
   *
   * @param caller   username of the caller
   * @param endpoint method and path of the endpoint
   * @param key      the idempotency key
   * @return the record, or empty when there is none
   */
  @Transactional(readOnly = true)
  public Optional<IdempotencyRecordPersistenceEntity> find(String caller, String endpoint, String key) {
    return repository.findByCallerAndEndpointAndIdempotencyKey(caller, endpoint, key);
  }

  /**
   * Claims a key for a request about to run, by inserting its record in progress.
   *
   * @param caller      username of the caller
   * @param endpoint    method and path of the endpoint
   * @param key         the idempotency key
   * @param requestHash digest of the request body
   * @return the identifier of the new record
   * @throws org.springframework.dao.DataIntegrityViolationException when the key is already claimed
   */
  @Transactional
  public Long claim(String caller, String endpoint, String key, String requestHash) {
    var now = Instant.now();
    var record = new IdempotencyRecordPersistenceEntity();
    record.setCaller(caller);
    record.setEndpoint(endpoint);
    record.setIdempotencyKey(key);
    record.setRequestHash(requestHash);
    record.setCompleted(false);
    record.setCreatedAt(now);
    record.setExpiresAt(now.plus(RETENTION));
    return repository.saveAndFlush(record).getId();
  }

  /**
   * Stores the answer of the request that claimed a key.
   *
   * @param recordId    the record of the claim
   * @param status      the HTTP status answered
   * @param contentType the content type answered, may be {@code null}
   * @param body        the body answered, secrets already removed
   */
  @Transactional
  public void complete(Long recordId, int status, String contentType, String body) {
    repository.findById(recordId).ifPresent(record -> {
      record.setCompleted(true);
      record.setResponseStatus(status);
      record.setResponseContentType(contentType);
      record.setResponseBody(body);
      repository.save(record);
    });
  }

  /**
   * Forgets a claim, so the key can be used again: the request failed and stored nothing.
   *
   * @param recordId the record of the claim
   */
  @Transactional
  public void release(Long recordId) {
    repository.deleteById(recordId);
  }

  /**
   * Forgets an expired record, so its key can be claimed again.
   *
   * @param recordId the record
   */
  @Transactional
  public void discard(Long recordId) {
    repository.deleteById(recordId);
  }

  /**
   * Deletes every record whose retention has elapsed.
   *
   * @return how many records were deleted
   */
  @Transactional
  public long purgeExpired() {
    return repository.delete(IdempotencyRecordPersistenceSpecifications.expiredBefore(Instant.now()));
  }
}
