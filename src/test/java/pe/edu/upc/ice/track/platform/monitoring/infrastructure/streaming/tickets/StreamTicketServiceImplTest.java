package pe.edu.upc.ice.track.platform.monitoring.infrastructure.streaming.tickets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.OWNER_PROFILE_ID;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.T0;

/** No collaborators to mock: the service is a self-contained in-memory store driven by an injectable clock. */
class StreamTicketServiceImplTest {

  private static final Long OTHER_OWNER_ID = 56L;
  private static final Duration TTL = StreamTicketServiceImpl.TICKET_TTL;

  /** A clock the tests can move forward. */
  private static final class MutableClock extends Clock {
    private Instant now;

    private MutableClock(Instant now) {
      this.now = now;
    }

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private MutableClock clock;
  private StreamTicketServiceImpl service;

  @BeforeEach
  void setUp() {
    clock = new MutableClock(T0);
    service = new StreamTicketServiceImpl(clock, 2, 100);
  }

  @Test
  @DisplayName("new StreamTicketServiceImpl(limits < 1) -> IllegalArgumentException (AAA)")
  void constructorRejectsInvalidLimits() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new StreamTicketServiceImpl(clock, 0, 10)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new StreamTicketServiceImpl(clock, 10, 0)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("new StreamTicketServiceImpl(null clock) -> NullPointerException (AAA)")
  void constructorRejectsNullClock() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> new StreamTicketServiceImpl(null, 1, 1)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("issue(ownerProfileId) -> an opaque st_ ticket valid for the TTL (AAA)")
  void issueReturnsTicket() {
    // Arrange & Act
    var issued = service.issue(OWNER_PROFILE_ID);

    // Assert
    assertThat(issued).isPresent();
    assertThat(issued.get().ticket()).matches("st_[0-9a-f]{64}");
    assertThat(issued.get().expiresIn()).isEqualTo(TTL);
    assertThat(service.pendingCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("issue(ownerProfileId) twice -> two different tickets (AAA)")
  void issueReturnsDistinctTickets() {
    // Arrange & Act
    var tickets = new HashSet<String>();
    tickets.add(service.issue(OWNER_PROFILE_ID).orElseThrow().ticket());
    tickets.add(service.issue(OWNER_PROFILE_ID).orElseThrow().ticket());

    // Assert
    assertThat(tickets).hasSize(2);
  }

  @Test
  @DisplayName("issue(null) -> NullPointerException (AAA)")
  void issueRejectsNullOwner() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> service.issue(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("issue(owner at the per-owner limit) -> empty, while other owners are unaffected (AAA)")
  void issueEnforcesPerOwnerLimit() {
    // Arrange
    service.issue(OWNER_PROFILE_ID);
    service.issue(OWNER_PROFILE_ID);

    // Act & Assert
    assertThat(service.issue(OWNER_PROFILE_ID)).isEmpty();
    assertThat(service.issue(OTHER_OWNER_ID)).isPresent();
  }

  @Test
  @DisplayName("issue(owner whose tickets expired) -> expired tickets free the quota (AAA)")
  void issueIgnoresExpiredTicketsInQuota() {
    // Arrange
    service.issue(OWNER_PROFILE_ID);
    service.issue(OWNER_PROFILE_ID);
    clock.advance(TTL);

    // Act
    var issued = service.issue(OWNER_PROFILE_ID);

    // Assert
    assertThat(issued).isPresent();
  }

  @Test
  @DisplayName("issue(store full of live tickets) -> empty (AAA)")
  void issueEnforcesGlobalLimit() {
    // Arrange
    var small = new StreamTicketServiceImpl(clock, 5, 2);
    small.issue(OWNER_PROFILE_ID);
    small.issue(OTHER_OWNER_ID);

    // Act & Assert
    assertThat(small.issue(3L)).isEmpty();
  }

  @Test
  @DisplayName("issue(store full of expired tickets) -> purges them and issues (AAA)")
  void issuePurgesExpiredWhenStoreIsFull() {
    // Arrange
    var small = new StreamTicketServiceImpl(clock, 5, 2);
    small.issue(OWNER_PROFILE_ID);
    small.issue(OTHER_OWNER_ID);
    clock.advance(TTL.plusSeconds(1));

    // Act
    var issued = small.issue(3L);

    // Assert
    assertThat(issued).isPresent();
    assertThat(small.pendingCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("redeem(valid ticket) -> owner profile id, and the ticket is single-use (AAA)")
  void redeemIsSingleUse() {
    // Arrange
    var ticket = service.issue(OWNER_PROFILE_ID).orElseThrow().ticket();

    // Act
    var first = service.redeem(ticket);
    var second = service.redeem(ticket);

    // Assert
    assertThat(first).contains(OWNER_PROFILE_ID);
    assertThat(second).isEmpty();
    assertThat(service.pendingCount()).isZero();
  }

  @Test
  @DisplayName("redeem(ticket with surrounding whitespace) -> still redeemed (AAA)")
  void redeemStripsWhitespace() {
    // Arrange
    var ticket = service.issue(OWNER_PROFILE_ID).orElseThrow().ticket();

    // Act
    var owner = service.redeem("  " + ticket + "\n");

    // Assert
    assertThat(owner).contains(OWNER_PROFILE_ID);
  }

  @Test
  @DisplayName("redeem(null or malformed) -> empty and leaves pending tickets alone (AAA)")
  void redeemRejectsMalformedTickets() {
    // Arrange
    var ticket = service.issue(OWNER_PROFILE_ID).orElseThrow().ticket();

    // Act & Assert
    assertThat(service.redeem(null)).isEmpty();
    assertThat(service.redeem("")).isEmpty();
    assertThat(service.redeem("not-a-ticket")).isEmpty();
    assertThat(service.redeem(ticket.toUpperCase())).isEmpty();
    assertThat(service.redeem(ticket.substring(0, ticket.length() - 1))).isEmpty();
    assertThat(service.redeem("xx_" + ticket.substring(3))).isEmpty();
    assertThat(service.pendingCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("redeem(well-formed but unknown ticket) -> empty (AAA)")
  void redeemRejectsUnknownTicket() {
    // Arrange
    var unknown = "st_" + "a".repeat(64);

    // Act & Assert
    assertThat(service.redeem(unknown)).isEmpty();
  }

  @Test
  @DisplayName("redeem(ticket one tick before expiry) -> valid; at expiry -> rejected (AAA)")
  void redeemHonoursExpiryBoundary() {
    // Arrange
    var beforeExpiry = service.issue(OWNER_PROFILE_ID).orElseThrow().ticket();
    var atExpiry = service.issue(OWNER_PROFILE_ID).orElseThrow().ticket();

    // Act
    clock.advance(TTL.minusMillis(1));
    var early = service.redeem(beforeExpiry);
    clock.advance(Duration.ofMillis(1));
    var late = service.redeem(atExpiry);

    // Assert
    assertThat(early).contains(OWNER_PROFILE_ID);
    assertThat(late).isEmpty();
  }

  @Test
  @DisplayName("purgeExpiredTickets() -> removes only the expired tickets (AAA)")
  void purgeRemovesOnlyExpired() {
    // Arrange
    service.issue(OWNER_PROFILE_ID);
    clock.advance(TTL.minusSeconds(10));
    var live = service.issue(OTHER_OWNER_ID).orElseThrow().ticket();
    clock.advance(Duration.ofSeconds(11));

    // Act
    service.purgeExpiredTickets();

    // Assert
    assertThat(service.pendingCount()).isEqualTo(1);
    assertThat(service.redeem(live)).contains(OTHER_OWNER_ID);
  }
}
