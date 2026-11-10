package ro.frit.ticketing.config;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.event.EventRepository;
import ro.frit.ticketing.event.EventService;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MongoPersistenceTest
@Import(EventService.class)
class EventVersionMigrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventService eventService;

    // Exactly what session 3 stored: an event document without a "version" field.
    @BeforeEach
    void anEventStoredBeforeOptimisticLocking() {
        eventRepository.deleteAll();
        mongoTemplate.getCollection("events").insertOne(new Document("_id", "old")
                .append("name", "Stored in session 3")
                .append("venue", new Document("name", "Hub").append("city", "Iasi"))
                .append("startsAt", Date.from(FixedClockConfiguration.NOW.plusSeconds(86_400)))
                .append("capacity", 10)
                .append("bookedSeats", 0));
    }

    @Test
    void anOldEventCanBeBookedAfterTheMigration() {
        new EventVersionMigration(mongoTemplate).run(null);

        eventService.reserveSeats("old", 2);

        assertEquals(2, eventRepository.findById("old").orElseThrow().getBookedSeats());
    }
}
