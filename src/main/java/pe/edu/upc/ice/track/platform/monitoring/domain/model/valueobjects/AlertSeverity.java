package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

public enum AlertSeverity {

  INFO(1),
  WARNING(2),
  CRITICAL(3);

  private final int value;

  AlertSeverity(int value){this.value=value;}

  public int getValue(){return value;}

  public static AlertSeverity fromValue(int value) {
    for (AlertSeverity severity : AlertSeverity.values()) {
      if (severity.value == value) {
        return severity;
      }
    }
    throw new IllegalArgumentException("Invalid AlertSeverity value: " + value);
  }
}
