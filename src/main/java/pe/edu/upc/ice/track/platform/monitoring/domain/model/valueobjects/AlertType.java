package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

/**
 *  Enumeration representing the types of alerts in the monitoring system.
 */
public enum AlertType {
  TEMPERATURE_EXCURSION(1),
  DEVICE_OFFLINE(2);

  private final int value;

  AlertType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static AlertType fromValue(int value) {
    for (AlertType type : AlertType.values()) {
      if (type.value == value) {
        return type;
      }
    }
    throw new IllegalArgumentException("Invalid AlertType value: " + value);
  }
}
