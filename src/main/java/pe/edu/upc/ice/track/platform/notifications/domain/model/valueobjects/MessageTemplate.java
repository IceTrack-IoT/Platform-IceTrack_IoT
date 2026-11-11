package pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The machine form of a notification text: a stable key naming the template, and the values to fill
 * into it.
 *
 * <p>Clients translate the template named by {@code key} into the user's language and fill in
 * {@code params}, keeping the English {@code message} as a fallback for a key they do not know.
 * Values are strings, numbers or booleans, never markup; free text such as a reason is user content
 * and is shown as typed.</p>
 *
 * <p>Adding a key is backward compatible. Changing the parameters of an existing key is not and needs
 * a new key.</p>
 *
 * @param key    the template key; required and never blank
 * @param params the values to fill in, in a stable order; {@code null} values are dropped
 */
public record MessageTemplate(String key, Map<String, Object> params) {

  /** A temperature excursion alert was raised on a unit. */
  public static final String ALERT_TEMPERATURE_EXCURSION_RAISED = "alert.temperature_excursion.raised";

  /** Offline detection raised an alert on a unit that stopped reporting. */
  public static final String ALERT_DEVICE_OFFLINE_RAISED = "alert.device_offline.raised";

  /** A sensor failure alert was raised on a unit. */
  public static final String ALERT_SENSOR_FAILURE_RAISED = "alert.sensor_failure.raised";

  /** A technician was assigned a service request. */
  public static final String SERVICE_REQUEST_ASSIGNED = "service_request.assigned";

  /** A technician was taken off a service request. */
  public static final String SERVICE_REQUEST_UNASSIGNED = "service_request.unassigned";

  /** The assigned technician rejected a service request. */
  public static final String SERVICE_REQUEST_REJECTED = "service_request.rejected";

  /** The owner canceled a service request. */
  public static final String SERVICE_REQUEST_CANCELED = "service_request.canceled";

  /** A maintenance window opened on a unit. */
  public static final String MAINTENANCE_WINDOW_OPENED = "maintenance_window.opened";

  /** A maintenance window closed on a unit. */
  public static final String MAINTENANCE_WINDOW_CLOSED = "maintenance_window.closed";

  /**
   * Key of a notification stored before templates were recorded. No client knows it, so the English
   * {@code message} is always shown for it.
   */
  public static final String UNTEMPLATED = "notification.untemplated";

  /**
   * Validates the key and keeps only the present values, unmodifiable and in insertion order.
   */
  public MessageTemplate {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("Message template key must not be null or blank");
    }
    var present = new LinkedHashMap<String, Object>();
    if (params != null) {
      params.forEach((name, value) -> {
        if (name != null && value != null) {
          present.put(name, value);
        }
      });
    }
    params = Collections.unmodifiableMap(present);
  }

  /**
   * The template of a notification stored before templates were recorded.
   *
   * @return a template with the {@link #UNTEMPLATED} key and no values
   */
  public static MessageTemplate untemplated() {
    return new MessageTemplate(UNTEMPLATED, Map.of());
  }

  /**
   * Starts a template with the given key.
   *
   * @param key the template key
   * @return a builder collecting the values in order
   */
  public static Builder of(String key) {
    return new Builder(Objects.requireNonNull(key, "key must not be null"));
  }

  /** Collects the values of a template in the order they are added. */
  public static final class Builder {
    private final String key;
    private final Map<String, Object> params = new LinkedHashMap<>();

    private Builder(String key) {
      this.key = key;
    }

    /**
     * Adds a value; a {@code null} value is left out.
     *
     * @param name  the parameter name, in {@code snake_case}
     * @param value the value
     * @return this builder
     */
    public Builder with(String name, Object value) {
      if (value != null) {
        params.put(name, value);
      }
      return this;
    }

    /**
     * Builds the template.
     *
     * @return the template
     */
    public MessageTemplate build() {
      return new MessageTemplate(key, params);
    }
  }
}
