package pe.edu.upc.ice.track.platform.shared.infrastructure.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.shared.application.outbox.IntegrationEventOutbox;
import pe.edu.upc.ice.track.platform.shared.infrastructure.outbox.persistence.jpa.entities.OutboxEventPersistenceEntity;
import pe.edu.upc.ice.track.platform.shared.infrastructure.outbox.persistence.jpa.repositories.OutboxEventPersistenceRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Objects;

/**
 * {@link IntegrationEventOutbox} storing events in the {@code outbox_events} table.
 *
 * <p>Appending requires the caller's transaction ({@link Propagation#MANDATORY}): an outbox row
 * written in a transaction of its own could outlive a rolled-back change, or be lost while the change
 * commits, which is exactly what the outbox exists to prevent. The payload is serialized with the
 * platform's {@link ObjectMapper}, the same one {@link OutboxEventDispatcher} reads it back with.</p>
 */
@Component
public class JpaIntegrationEventOutbox implements IntegrationEventOutbox {

  private final OutboxEventPersistenceRepository repository;
  private final ObjectMapper objectMapper;

  public JpaIntegrationEventOutbox(OutboxEventPersistenceRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void append(String aggregateType, Long aggregateId, Object integrationEvent) {
    Objects.requireNonNull(aggregateType, "aggregateType must not be null");
    Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    Objects.requireNonNull(integrationEvent, "integrationEvent must not be null");
    var entity = new OutboxEventPersistenceEntity();
    entity.setAggregateType(aggregateType);
    entity.setAggregateId(aggregateId);
    entity.setEventType(integrationEvent.getClass().getName());
    entity.setPayloadJson(objectMapper.writeValueAsString(integrationEvent));
    entity.setCreatedAt(Instant.now());
    entity.setAttempts(0);
    repository.save(entity);
  }
}
