package ro.frit.ticketing.event;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ro.frit.ticketing.MongoPersistenceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ro.frit.ticketing.config.FixedClockConfiguration.TODAY_AT_NOON;
import static ro.frit.ticketing.event.EventFixtures.event;

@MongoPersistenceTest
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void emptyCollection() {
        eventRepository.deleteAll();
    }

    @Test
    void anEventIsStoredWithItsVenueAndReadBackTheSame() {
        Event stored = eventRepository.save(event("e1", "Cluj-Napoca", TODAY_AT_NOON.plusDays(3), 30, 4));

        Event read = eventRepository.findById("e1").orElseThrow();

        assertEquals(stored.getVenue(), read.getVenue());
        assertEquals(stored.getStartsAt(), read.getStartsAt());
        assertEquals(26, read.availableSeats());
    }

    @Test
    void anInvalidEventIsNotSaved() {
        Event invalid = event("bad", "Iasi", TODAY_AT_NOON.plusDays(2), 0, 0); // capacity must be positive

        assertThrows(ConstraintViolationException.class, () -> eventRepository.save(invalid));
        assertEquals(0, eventRepository.count());
    }
}
