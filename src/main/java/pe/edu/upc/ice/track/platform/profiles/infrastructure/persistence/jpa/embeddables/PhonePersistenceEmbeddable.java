package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class PhonePersistenceEmbeddable {

  @Column(name = "country_code")
  private String countryCode;

  @Column(name = "phone_number")
  private String number;

  public PhonePersistenceEmbeddable() {
  }

  public PhonePersistenceEmbeddable(String countryCode, String number) {
    this.countryCode = countryCode;
    this.number = number;
  }
}
