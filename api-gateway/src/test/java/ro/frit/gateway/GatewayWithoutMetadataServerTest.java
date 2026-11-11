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

// Cloud Run auth on, but no metadata server answers: the client gets a 503 ProblemDetail, not a stack trace,
// and the request never reaches the service without its token.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "gateway.cloud-run-auth.enabled=true")
class GatewayWithoutMetadataServerTest {

    private static final StubService ticketing = new StubService("ticketing", "Authorization");

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void pointAtNothing(DynamicPropertyRegistry registry) {
        FakeMetadataServer gone = new FakeMetadataServer();
        gone.stop(); // a port where nothing listens any more
        registry.add("gateway.cloud-run-auth.metadata-uri", gone::uri);
        registry.add("gateway.ticketing-uri", ticketing::uri);
        registry.add("gateway.notifications-uri", ticketing::uri);
    }

    @AfterAll
    static void stopStub() {
        ticketing.stop();
    }

    @Test
    void withoutATokenTheClientGetsServiceUnavailable() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/events")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(503, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"status\":503"), response.body());
    }
}
