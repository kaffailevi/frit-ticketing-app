package ro.frit.gateway;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

// A limit of 3 requests per minute: the 4th from the same client is refused with 429 and Retry-After;
// another client still gets through. Each test uses its own client key (X-Forwarded-For), so the buckets are fresh.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRateLimitTest {

    private static final StubService ticketing = new StubService("ticketing", "X-Test");

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void aLowLimit(DynamicPropertyRegistry registry) {
        registry.add("gateway.ticketing-uri", ticketing::uri);
        registry.add("gateway.notifications-uri", ticketing::uri);
        registry.add("gateway.rate-limit.capacity", () -> 3);
        registry.add("gateway.rate-limit.period", () -> "1m");
    }

    @AfterAll
    static void stopStub() {
        ticketing.stop();
    }

    private HttpResponse<String> getEventsAs(String clientAddress) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/events"))
                .header("X-Forwarded-For", clientAddress)
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void theRequestAfterTheLimitIsRefusedWithRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(200, getEventsAs("10.0.0.1").statusCode());
        }

        HttpResponse<String> refused = getEventsAs("10.0.0.1");

        assertEquals(429, refused.statusCode());
        assertEquals("20", refused.headers().firstValue("Retry-After").orElseThrow()); // 60 s / 3 tokens
    }

    @Test
    void eachClientHasItsOwnLimit() throws Exception {
        for (int i = 0; i < 4; i++) {
            getEventsAs("10.0.0.2");
        }

        assertEquals(200, getEventsAs("10.0.0.3").statusCode());
    }
}
