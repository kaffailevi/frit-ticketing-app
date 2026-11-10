package ro.frit.ticketing.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.event.EventRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ro.frit.ticketing.event.EventFixtures.tomorrowEvening;

@MongoPersistenceTest
class DataSeederTest {

    private static final TicketingProperties TWO_SEEDS = new TicketingProperties(List.of(
            new TicketingProperties.SeedEvent("react-cluj", "React Workshop", "Hub", "Cluj-Napoca",
                    LocalDateTime.of(2027, 2, 10, 18, 0), 30),
            new TicketingProperties.SeedEvent("cloud-iasi", "Cloud Run Hands-on", "Palas", "Iasi",
                    LocalDateTime.of(2027, 3, 5, 10, 0), 40)));

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void emptyCollection() {
        eventRepository.deleteAll();
    }

    @Test
    void anEmptyDatabaseGetsTheSeedEventsWithAllSeatsFree() {
        new DataSeeder(TWO_SEEDS, eventRepository).run(null);

        assertEquals(2, eventRepository.count());
        assertEquals(40, eventRepository.findById("cloud-iasi").orElseThrow().availableSeats());
    }

    @Test
    void storedEventsAreNeverOverwrittenByTheSeeds() {
        eventRepository.save(tomorrowEvening("react-cluj"));

        new DataSeeder(TWO_SEEDS, eventRepository).run(null);

        assertEquals(1, eventRepository.count());
        assertEquals(8, eventRepository.findById("react-cluj").orElseThrow().getBookedSeats());
    }
}
