package ro.frit.ticketing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.config.TestcontainersConfiguration;
import ro.frit.ticketing.event.EventRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The whole application starts against a MongoDB in Docker, and DataSeeder stores the events from application.yml.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TicketingApplicationTest {

    @Autowired
    private EventRepository eventRepository;

    @Test
    void theApplicationStartsWithTheLearningEventsFromTheConfiguration() {
        assertEquals("Spring Boot 4 Meetup", eventRepository.findById("spring-cluj").orElseThrow().getName());
    }
}
