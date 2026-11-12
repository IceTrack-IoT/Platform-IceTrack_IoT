package pe.edu.upc.ice.track.platform.profiles.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.OwnerCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.TechnicianCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.interfaces.events.OwnerCreatedIntegrationEvent;
import pe.edu.upc.ice.track.platform.profiles.interfaces.events.TechnicianCreatedIntegrationEvent;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.CERTIFICATION;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.EMAIL;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OWNER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.RUC;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.TECHNICIAN_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;

class ProfileCreatedEventHandlersTest {

  @Nested
  @ExtendWith(MockitoExtension.class)
  class OwnerCreatedEventHandlerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private OwnerCreatedEventHandler handler;

    @Test
    @DisplayName("on(OwnerCreatedEvent) -> republishes it as the integration event, field by field (AAA)")
    void republishes() {
      // Arrange
      var event = new OwnerCreatedEvent(OWNER_ID, USER_ID, "Ana Torres", EMAIL, RUC);

      // Act
      handler.on(event);

      // Assert
      verify(eventPublisher, times(1)).publishEvent(
          new OwnerCreatedIntegrationEvent(OWNER_ID, USER_ID, "Ana Torres", EMAIL, RUC));
      verifyNoMoreInteractions(eventPublisher);
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class TechnicianCreatedEventHandlerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private TechnicianCreatedEventHandler handler;

    @Test
    @DisplayName("on(TechnicianCreatedEvent) -> republishes it as the integration event, field by field (AAA)")
    void republishes() {
      // Arrange
      var event = new TechnicianCreatedEvent(TECHNICIAN_ID, USER_ID, "Ana Torres", EMAIL, "Refrigeration", CERTIFICATION);

      // Act
      handler.on(event);

      // Assert
      verify(eventPublisher, times(1)).publishEvent(new TechnicianCreatedIntegrationEvent(
          TECHNICIAN_ID, USER_ID, "Ana Torres", EMAIL, "Refrigeration", CERTIFICATION));
      verifyNoMoreInteractions(eventPublisher);
    }
  }
}
