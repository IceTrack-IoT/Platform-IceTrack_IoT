package pe.edu.upc.ice.track.platform.shared.infrastructure.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upc.ice.track.platform.shared.infrastructure.outbox.persistence.jpa.entities.OutboxEventPersistenceEntity;
import pe.edu.upc.ice.track.platform.shared.infrastructure.outbox.persistence.jpa.repositories.OutboxEventPersistenceRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scheduled dispatcher delivering the integration events of the transactional outbox.
 *
 * <p>Every {@code outbox.dispatcher.fixed-delay-ms} it takes the oldest pending events and delivers
 * each in a transaction of its own: the row is locked, the event rebuilt from its JSON and published
 * on the Spring event bus, and {@code published_at} set. Consumers subscribe with a plain
 * {@code @EventListener}, so they run inside that transaction: a consumer that fails rolls back its
 * own writes together with the {@code published_at} stamp, and the event is retried on a later run.
 * Delivery is therefore at least once, and consumers are idempotent.</p>
 *
 * <p>A failed delivery increments {@code attempts} and keeps the failure in {@code last_error}. An
 * event that failed {@code outbox.dispatcher.max-attempts} times is no longer tried; it stays in the
 * table, unpublished, for somebody to look into. One failing event never holds back the others.</p>
 *
 * <p>Only records of a context's {@code interfaces.events} package are rebuilt: the stored type name
 * is checked against that shape before any class is loaded.</p>
 */
@Component
@Slf4j
public class OutboxEventDispatcher {

  private static final Pattern INTEGRATION_EVENT_TYPE =
      Pattern.compile("^pe\\.edu\\.upc\\.ice\\.track\\.platform\\.[a-z.]+\\.interfaces\\.events\\.[A-Za-z]+IntegrationEvent$");

  private final OutboxEventPersistenceRepository repository;
  private final ApplicationEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;
  private final int maxAttempts;

  public OutboxEventDispatcher(
      OutboxEventPersistenceRepository repository,
      ApplicationEventPublisher eventPublisher,
      ObjectMapper objectMapper,
      PlatformTransactionManager transactionManager,
      @Value("${outbox.dispatcher.max-attempts:10}") int maxAttempts) {
    if (maxAttempts < 1) {
      throw new IllegalStateException("outbox.dispatcher.max-attempts must be at least 1");
    }
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.objectMapper = objectMapper;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.maxAttempts = maxAttempts;
  }

  /**
   * Delivers the pending events, oldest first. Never throws, so the next run still takes place.
   */
  @Scheduled(fixedDelayString = "${outbox.dispatcher.fixed-delay-ms:2000}")
  public void dispatchPendingEvents() {
    final List<Long> pending;
    try {
      pending = transactionTemplate.execute(status ->
          repository.findTop100ByPublishedAtIsNullAndAttemptsLessThanOrderByIdAsc(maxAttempts).stream()
              .map(OutboxEventPersistenceEntity::getId)
              .toList());
    } catch (RuntimeException e) {
      log.error("Could not read the pending outbox events: {}", e.getMessage(), e);
      return;
    }
    if (pending == null) {
      return;
    }
    pending.forEach(this::dispatch);
  }

  /**
   * Delivers one event in a transaction of its own, recording the failure when the delivery fails.
   *
   * @param id the event identity
   */
  private void dispatch(Long id) {
    try {
      transactionTemplate.executeWithoutResult(status -> repository.findByIdAndPublishedAtIsNull(id).ifPresent(row -> {
        eventPublisher.publishEvent(rebuild(row));
        row.setPublishedAt(Instant.now());
      }));
    } catch (RuntimeException e) {
      log.warn("Delivery of outbox event {} failed: {}", id, e.getMessage(), e);
      recordFailure(id, e);
    }
  }

  private void recordFailure(Long id, RuntimeException failure) {
    try {
      transactionTemplate.executeWithoutResult(status -> repository.findById(id).ifPresent(row -> {
        row.setAttempts(row.getAttempts() + 1);
        row.setLastError(truncated(failure.getClass().getSimpleName() + ": " + failure.getMessage()));
        if (row.getAttempts() >= maxAttempts) {
          log.error("Outbox event {} ({}) failed {} times and will no longer be delivered",
              id, row.getEventType(), row.getAttempts());
        }
      }));
    } catch (RuntimeException e) {
      log.error("Could not record the failed delivery of outbox event {}: {}", id, e.getMessage(), e);
    }
  }

  private Object rebuild(OutboxEventPersistenceEntity row) {
    var eventType = row.getEventType();
    if (eventType == null || !INTEGRATION_EVENT_TYPE.matcher(eventType).matches()) {
      throw new IllegalStateException("Outbox event type %s is not an integration event".formatted(eventType));
    }
    try {
      return objectMapper.readValue(row.getPayloadJson(), Class.forName(eventType));
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Outbox event type %s no longer exists".formatted(eventType), e);
    }
  }

  private static String truncated(String message) {
    return message.length() <= OutboxEventPersistenceEntity.MAX_ERROR_LENGTH
        ? message
        : message.substring(0, OutboxEventPersistenceEntity.MAX_ERROR_LENGTH);
  }
}
