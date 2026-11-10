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

// The gateway on a random port, the services replaced by StubServices: each path reaches its own service, unchanged.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTest {

    private static final StubService ticketing = new StubService("ticketing", "X-Test");
    private static final StubService notifications = new StubService("notifications", "X-Test");

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void routeToTheStubs(DynamicPropertyRegistry registry) {
        registry.add("gateway.ticketing-uri", ticketing::uri);
        registry.add("gateway.notifications-uri", notifications::uri);
    }

    @AfterAll
    static void stopStubs() {
        ticketing.stop();
        notifications.stop();
    }

    private HttpResponse<String> send(String method, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("X-Test", "passed-on")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void eventsGoToTicketingWithTheirQueryAndHeaders() throws Exception {
        HttpResponse<String> response = send("GET", "/events?city=Iasi");

        assertEquals(200, response.statusCode());
        assertEquals("ticketing GET /events?city=Iasi passed-on", response.body());
    }

    @Test
    void bookingsGoToTicketingWithTheirMethod() throws Exception {
        assertEquals("ticketing POST /events/react-cluj/bookings passed-on",
                send("POST", "/events/react-cluj/bookings").body());
    }

    @Test
    void notificationsGoToTheNotificationService() throws Exception {
        // The gateway forwards the query URL-encoded (@ as %40); the service decodes it back to the same email.
        assertEquals("notifications GET /notifications?customerEmail=ana%40example.com passed-on",
                send("GET", "/notifications?customerEmail=ana@example.com").body());
    }

    @Test
    void aPathWithoutARouteIsNotFound() throws Exception {
        assertEquals(404, send("GET", "/nowhere").statusCode());
    }

    @Test
    void theGatewayReportsItsOwnHealth() throws Exception {
        assertEquals(200, send("GET", "/actuator/health").statusCode());
    }
}
