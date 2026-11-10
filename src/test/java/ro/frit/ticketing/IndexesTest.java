package ro.frit.ticketing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import ro.frit.ticketing.booking.Booking;
import ro.frit.ticketing.event.Event;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

// The indexes declared on the documents exist in MongoDB (spring.data.mongodb.auto-index-creation).
@MongoPersistenceTest
class IndexesTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    private List<String> indexNames(Class<?> document) {
        return mongoTemplate.indexOps(document).getIndexInfo().stream().map(IndexInfo::getName).toList();
    }

    @Test
    void bookingsAreIndexedByEvent() {
        assertTrue(indexNames(Booking.class).contains("eventId"), "indexes: " + indexNames(Booking.class));
    }

    @Test
    void eventsAreIndexedByCityAndStartTime() {
        assertTrue(indexNames(Event.class).contains("city_startsAt"), "indexes: " + indexNames(Event.class));
    }
}
