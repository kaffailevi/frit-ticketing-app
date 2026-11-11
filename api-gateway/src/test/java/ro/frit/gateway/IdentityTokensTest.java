package ro.frit.gateway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// How the gateway gets and keeps its ID tokens, against a fake metadata server and a clock the test moves.
class IdentityTokensTest {

    private static final URI TICKETING = URI.create("https://ticketing-service-abc.a.run.app");
    private static final URI NOTIFICATIONS = URI.create("https://notification-service-abc.a.run.app");

    private final FakeMetadataServer metadataServer = new FakeMetadataServer();
    private final MovableClock clock = new MovableClock(Instant.parse("2026-11-17T15:00:00Z"));
    private final IdentityTokens identityTokens = new IdentityTokens(
            new GatewayProperties.CloudRunAuth(true, URI.create(metadataServer.uri())), clock);

    @AfterEach
    void stopServer() {
        metadataServer.stop();
    }

    @Test
    void eachServiceGetsATokenForItsOwnAddress() {
        assertEquals("token-1-for-" + TICKETING, identityTokens.tokenFor(TICKETING));
        assertEquals("token-2-for-" + NOTIFICATIONS, identityTokens.tokenFor(NOTIFICATIONS));
    }

    @Test
    void aTokenIsReusedForFiftyMinutes() {
        identityTokens.tokenFor(TICKETING);
        clock.move(Duration.ofMinutes(49));

        assertEquals("token-1-for-" + TICKETING, identityTokens.tokenFor(TICKETING));
        assertEquals(1, metadataServer.tokensHandedOut());
    }

    @Test
    void afterFiftyMinutesANewTokenIsFetched() {
        identityTokens.tokenFor(TICKETING);
        clock.move(Duration.ofMinutes(50));

        assertEquals("token-2-for-" + TICKETING, identityTokens.tokenFor(TICKETING));
    }

    @Test
    void withoutAMetadataServerThereIsNoToken() {
        metadataServer.stop();

        assertThrows(RestClientException.class, () -> identityTokens.tokenFor(TICKETING));
    }

    private static final class MovableClock extends Clock {

        private Instant now;

        MovableClock(Instant now) {
            this.now = now;
        }

        void move(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
