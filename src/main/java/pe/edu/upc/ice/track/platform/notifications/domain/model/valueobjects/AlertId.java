package pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

/**
 *  Value object representing the identifier of an {@code Alert} in the Monitoring and Alerting Management bounded context.
 * @param alertId the identifier of the alert; must not be null
 */
@Embeddable
public record AlertId(Long alertId) {

  public AlertId{
    if (alertId == null){
      throw new IllegalArgumentException("AlertId cannot be null");
    }
  }
}
