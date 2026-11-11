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

// The gateway as it runs on Cloud Run, with a fake metadata server: every request reaches its service carrying
// that service's ID token. The stubs answer with the Authorization header they received.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "gateway.cloud-run-auth.enabled=true")
class GatewayCloudRunAuthTest {

    private static final FakeMetadataServer metadataServer = new FakeMetadataServer();
    private static final StubService ticketing = new StubService("ticketing", "Authorization");
    private static final StubService notifications = new StubService("notifications", "Authorization");

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void pointAtTheFakes(DynamicPropertyRegistry registry) {
        registry.add("gateway.cloud-run-auth.metadata-uri", metadataServer::uri);
        registry.add("gateway.ticketing-uri", ticketing::uri);
        registry.add("gateway.notifications-uri", notifications::uri);
    }

    @AfterAll
    static void stopFakes() {
        metadataServer.stop();
        ticketing.stop();
        notifications.stop();
    }

    private String get(String path, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (headers.length > 0) {
            request.headers(headers);
        }
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return response.body();
    }

    @Test
    void eachServiceReceivesATokenForItself() throws Exception {
        assertTrue(get("/events").matches("ticketing GET /events Bearer token-\\d+-for-" + ticketing.uri()), "ticketing");
        assertTrue(get("/notifications").matches("notifications GET /notifications Bearer token-\\d+-for-" + notifications.uri()), "notifications");
    }

    @Test
    void theClientsOwnAuthorizationHeaderNeverReachesAService() throws Exception {
        String received = get("/events", "Authorization", "Bearer something-a-client-made-up");

        assertTrue(received.contains("for-" + ticketing.uri()), received);
    }
}
