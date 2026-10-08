package pe.edu.upc.ice.track.platform.assets.infrastructure.persistence.jpa.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.Phone;

/**
 * Converts a contact phone number between the domain model and the persisted column value.
 *
 * <p>Stored as text so that an international prefix is preserved verbatim instead of being parsed
 * into a number and re-rendered in a locale-dependent form.</p>
 */
@Converter(autoApply = false)
public class PhonePersistenceConverter implements AttributeConverter<Phone, String> {

  /**
   * Returns the raw phone number to store.
   *
   * @param attribute the phone value object, may be {@code null}
   * @return the persisted text
   */
  @Override
  public String convertToDatabaseColumn(Phone attribute) {
    return attribute == null ? null : attribute.value();
  }

  /**
   * Rebuilds the phone value object from the persisted text.
   *
   * @param dbData the persisted text, may be {@code null}
   * @return the phone value object
   */
  @Override
  public Phone convertToEntityAttribute(String dbData) {
    return dbData == null ? null : new Phone(dbData);
  }
}