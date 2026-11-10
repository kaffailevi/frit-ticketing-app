package ro.frit.ticketing.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTest {

    private static final LocalDateTime EVENING = LocalDateTime.of(2026, 12, 1, 19, 0);

    private static Event.EventBuilder workshop() {
        return Event.builder()
                .id("e1")
                .name("Spring Boot workshop")
                .venue(new Venue("Hub", "Cluj-Napoca"))
                .startsAt(EVENING)
                .capacity(10)
                .bookedSeats(3);
    }

    @Test
    void availableSeatsAreTheCapacityMinusTheBookedSeats() {
        assertEquals(7, workshop().build().availableSeats());
    }

    @Test
    void bookingSeatsGivesANewEventAndLeavesTheOldOneUnchanged() {
        Event event = workshop().build();

        Event booked = event.withSeatsBooked(2);

        assertEquals(5, booked.getBookedSeats());
        assertEquals(3, event.getBookedSeats());
    }

    @Test
    void anEventHasStartedAtItsStartTime() {
        assertTrue(workshop().build().hasStartedAt(EVENING));
    }

    @Test
    void anEventHasNotStartedBeforeItsStartTime() {
        assertFalse(workshop().build().hasStartedAt(EVENING.minusMinutes(1)));
    }

    @Test
    void twoEventsWithTheSameIdAreTheSameEvent() {
        assertEquals(workshop().build(), workshop().name("Renamed").bookedSeats(9).build());
    }

    @Test
    void twoEventsWithDifferentIdsAreDifferentEvents() {
        assertNotEquals(workshop().build(), workshop().id("e2").build());
    }
}
