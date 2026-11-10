package pe.edu.upc.ice.track.platform.shared.application.outbox;

/**
 * Port of the transactional outbox through which a bounded context hands integration events to the
 * rest of the platform with guaranteed delivery.
 *
 * <p>An event appended here is stored in the same transaction as the change it announces, so it is
 * delivered if and only if that change commits. A dispatcher then publishes it on the platform's
 * event bus, retrying until a delivery succeeds; consumers must therefore tolerate receiving an event
 * more than once.</p>
 */
public interface IntegrationEventOutbox {

  /**
   * Stores an integration event for delivery, in the caller's transaction.
   *
   * @param aggregateType   the kind of aggregate the event is about, such as {@code ServiceRequest}
   * @param aggregateId     identity of that aggregate
   * @param integrationEvent the integration event, a record of some context's {@code interfaces.events}
   *                        package
   * @throws org.springframework.transaction.IllegalTransactionStateException when no transaction is
   *                                                                          active
   */
  void append(String aggregateType, Long aggregateId, Object integrationEvent);
}
