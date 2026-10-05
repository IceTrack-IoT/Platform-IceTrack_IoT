package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Address;

/**
 * Converts a site address between the domain model and the persisted column value.
 *
 * <p>A converter rather than an embeddable: the address is a single opaque line, so embedding it
 * would only buy a wrapper around one column while making the generated DDL harder to read.</p>
 */
@Converter(autoApply = false)
public class AddressPersistenceConverter implements AttributeConverter<Address, String> {

  /**
   * Returns the raw address line to store.
   *
   * @param attribute the address value object, may be {@code null}
   * @return the persisted text
   */
  @Override
  public String convertToDatabaseColumn(Address attribute) {
    return attribute == null ? null : attribute.value();
  }

  /**
   * Rebuilds the address value object from the persisted text.
   *
   * @param dbData the persisted text, may be {@code null}
   * @return the address value object
   */
  @Override
  public Address convertToEntityAttribute(String dbData) {
    return dbData == null ? null : new Address(dbData);
  }
}