package ro.frit.ticketing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.config.RabbitTestcontainersConfiguration;
import ro.frit.ticketing.config.TestcontainersConfiguration;
import ro.frit.ticketing.event.EventRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The whole application starts against a MongoDB in Docker, and DataSeeder stores the events from application.yml.
// Its own database: the container is shared by all test classes, and the seeder skips a database that has events.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.mongodb.database=application-start-test")
@Import({TestcontainersConfiguration.class, RabbitTestcontainersConfiguration.class})
class TicketingApplicationTest {

    @Autowired
    private EventRepository eventRepository;

    @LocalServerPort
    private int port;

    @Test
    void theApplicationStartsWithTheLearningEventsFromTheConfiguration() {
        assertEquals("Spring Boot 4 Meetup", eventRepository.findById("spring-cluj").orElseThrow().getName());
    }

    // What Docker's HEALTHCHECK and Cloud Run's startup probe ask before they send traffic.
    @Test
    void theStartedApplicationSaysItIsReady() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health/readiness")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"UP\""), response.body());
    }
}
