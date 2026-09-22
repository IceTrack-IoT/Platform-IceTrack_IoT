package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistence representation for a person name.
 */
@Getter
@Setter
@Embeddable
public class PersonNamePersistenceEmbeddable {

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  public PersonNamePersistenceEmbeddable() {
  }

  public PersonNamePersistenceEmbeddable(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
  }
}
