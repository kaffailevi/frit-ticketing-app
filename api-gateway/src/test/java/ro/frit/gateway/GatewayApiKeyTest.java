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

// With an API key configured, only requests that send it reach a service. (The other gateway tests run without one.)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "gateway.api-key=test-key-123")
class GatewayApiKeyTest {

    private static final StubService ticketing = new StubService("ticketing", "X-Test");

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

    private HttpResponse<String> get(String path, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (headers.length > 0) {
            request.headers(headers);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void aRequestWithTheKeyReachesTheService() throws Exception {
        assertEquals(200, get("/events", "X-Api-Key", "test-key-123").statusCode());
    }

    @Test
    void aRequestWithoutTheKeyIsUnauthorizedAsAProblemDetail() throws Exception {
        HttpResponse<String> response = get("/events");

        assertEquals(401, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"status\":401"), response.body());
    }

    @Test
    void aWrongKeyIsUnauthorized() throws Exception {
        assertEquals(401, get("/notifications", "X-Api-Key", "guess").statusCode());
    }

    @Test
    void healthChecksNeedNoKey() throws Exception {
        assertEquals(200, get("/actuator/health").statusCode());
    }

    @Test
    void evenARefusedRequestGetsACorrelationId() throws Exception {
        assertTrue(get("/events").headers().firstValue(CorrelationIdFilter.HEADER).isPresent());
    }
}
