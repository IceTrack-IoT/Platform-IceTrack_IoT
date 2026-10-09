package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class TemperatureRangePersistenceEmbeddable {

  @Column(name = "temperature_range_min")
  private Integer min;

  @Column(name = "temperature_range_max")
  private Integer max;

  @Column(name = "temperature_range_unit")
  private String unit;

  @Column(name = "temperature_range_label")
  private String label;

  public TemperatureRangePersistenceEmbeddable() {
  }

  public TemperatureRangePersistenceEmbeddable(Integer min, Integer max, String unit, String label) {
    this.min = min;
    this.max = max;
    this.unit = unit;
    this.label = label;
  }
}
