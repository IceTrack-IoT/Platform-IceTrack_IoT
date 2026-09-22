package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistence representation for a street address.
 */
@Getter
@Setter
@Embeddable
public class StreetAddressPersistenceEmbeddable {

  @Column(name = "street_address_street")
  private String street;

  @Column(name = "street_address_number")
  private String number;

  @Column(name = "street_address_city")
  private String city;

  @Column(name = "street_address_postal_code")
  private String postalCode;

  @Column(name = "street_address_country")
  private String country;

  public StreetAddressPersistenceEmbeddable() {
  }

  public StreetAddressPersistenceEmbeddable(String street, String number, String city, String postalCode, String country) {
    this.street = street;
    this.number = number;
    this.city = city;
    this.postalCode = postalCode;
    this.country = country;
  }
}
