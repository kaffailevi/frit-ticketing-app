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
import static org.junit.jupiter.api.Assertions.assertTrue;

// The stub service echoes the X-Correlation-Id it received, so the test sees what the gateway forwarded.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayCorrelationIdTest {

    private static final StubService ticketing = new StubService("ticketing", CorrelationIdFilter.HEADER);

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void routeToTheStub(DynamicPropertyRegistry registry) {
        registry.add("gateway.ticketing-uri", ticketing::uri);
        registry.add("gateway.notifications-uri", ticketing::uri);
    }

    @AfterAll
    static void stopStub() {
        ticketing.stop();
    }

    private HttpResponse<String> getEvents(String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/events"));
        if (headers.length > 0) {
            request.headers(headers);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void theClientsIdIsForwardedAndReturned() throws Exception {
        HttpResponse<String> response = getEvents(CorrelationIdFilter.HEADER, "skillforge-123");

        assertEquals("ticketing GET /events skillforge-123", response.body());
        assertEquals("skillforge-123", response.headers().firstValue(CorrelationIdFilter.HEADER).orElseThrow());
    }

    @Test
    void withoutAnIdTheGatewayMakesOneAndUsesItBothWays() throws Exception {
        HttpResponse<String> response = getEvents();

        String returned = response.headers().firstValue(CorrelationIdFilter.HEADER).orElseThrow();
        assertTrue(returned.matches("[0-9a-f-]{36}"), returned);
        assertEquals("ticketing GET /events " + returned, response.body());
    }
}
