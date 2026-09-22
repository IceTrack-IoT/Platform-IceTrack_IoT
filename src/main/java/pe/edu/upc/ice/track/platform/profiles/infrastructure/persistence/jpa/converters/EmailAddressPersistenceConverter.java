package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;

/**
 * Converts email addresses between the domain model and persistence column values.
 */
@Converter(autoApply = false)
public class EmailAddressPersistenceConverter implements AttributeConverter<EmailAddress, String> {

  @Override
  public String convertToDatabaseColumn(EmailAddress attribute) {
    return attribute == null ? null : attribute.address();
  }

  @Override
  public EmailAddress convertToEntityAttribute(String dbData) {
    return dbData == null ? null : new EmailAddress(dbData);
  }
}
