package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class TemperatureRangePersistenceEmbeddable {

  @Column(name = "temperature_range_value")
  private String value;

  @Column(name = "temperature_range_label")
  private String label;

  public TemperatureRangePersistenceEmbeddable() {
  }

  public TemperatureRangePersistenceEmbeddable(String value, String label) {
    this.value = value;
    this.label = label;
  }
}
