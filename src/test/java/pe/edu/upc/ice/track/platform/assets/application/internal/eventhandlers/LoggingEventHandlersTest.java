package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.SiteCreatedEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.TemperatureThresholdUpdatedEvent;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Handlers that only log have no collaborators to verify; the contract under test is that handling
 * a well formed event never throws and so can never break the publishing transaction.
 */
class LoggingEventHandlersTest {

  @Test
  @DisplayName("SiteCreatedEventHandler.on(SiteCreatedEvent) -> handles the event without failing (AAA)")
  void siteCreatedDoesNotThrow() {
    // Arrange
    var handler = new SiteCreatedEventHandler();
    var event = SiteCreatedEvent.from(AssetsDomainFixtures.site());

    // Act & Assert
    assertDoesNotThrow(() -> handler.on(event));
  }

  @Test
  @DisplayName("TemperatureThresholdUpdatedEventHandler.on(TemperatureThresholdUpdatedEvent) -> handles the event without failing (AAA)")
  void thresholdUpdatedDoesNotThrow() {
    // Arrange
    var handler = new TemperatureThresholdUpdatedEventHandler();
    var event = TemperatureThresholdUpdatedEvent.from(AssetsDomainFixtures.equipment());

    // Act & Assert
    assertDoesNotThrow(() -> handler.on(event));
  }
}
