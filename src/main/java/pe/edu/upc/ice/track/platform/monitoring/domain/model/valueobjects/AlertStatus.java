package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

/**
 *  Enumeration representing the status of an alert in the monitoring system.
 */
public enum AlertStatus {

  OPEN(1),
  ACKNOWLEDGED(2),
  RESOLVED(3),
  DISMISSED(4);

  private final int value;

  AlertStatus(int value){this.value=value;}

  public int getValue(){return value;}

  public static AlertStatus fromValue(int value) {
    for (AlertStatus status : AlertStatus.values()) {
      if (status.ordinal() == value) {
        return status;
      }
    }
    throw new IllegalArgumentException("Invalid AlertStatus value: " + value);
  }
}
