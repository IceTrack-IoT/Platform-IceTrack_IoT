package pe.edu.upc.ice.track.platform.notifications.infrastructure.persistence.jpa.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores the values of a notification template as a JSON object in one text column.
 *
 * <p>The values are strings, numbers and booleans only, so the plain JSON form reads back to the same
 * values. A column that cannot be read, which only a hand-edited row could produce, reads back as no
 * values rather than failing the whole inbox.</p>
 */
@Converter
public class MessageParamsPersistenceConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<LinkedHashMap<String, Object>> PARAMS_TYPE = new TypeReference<>() {
  };

  @Override
  public String convertToDatabaseColumn(Map<String, Object> params) {
    if (params == null || params.isEmpty()) {
      return "{}";
    }
    return JSON.writeValueAsString(params);
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(String column) {
    if (column == null || column.isBlank()) {
      return Map.of();
    }
    try {
      return JSON.readValue(column, PARAMS_TYPE);
    } catch (JacksonException e) {
      return Map.of();
    }
  }
}
