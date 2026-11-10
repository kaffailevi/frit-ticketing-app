package ro.frit.ticketing.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketOfficeQueriesTest {

    private static final Instant NOW = Instant.parse("2026-11-30T12:00:00Z");
    private static final LocalDateTime TODAY = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

    private static Event event(String id, String name, String city, LocalDateTime startsAt, int bookedSeats) {
        return Event.builder()
                .id(id)
                .name(name)
                .venue(new Venue("Venue " + id, city))
                .startsAt(startsAt)
                .capacity(10)
                .bookedSeats(bookedSeats)
                .build();
    }

    private final Event react = event("e1", "React meetup", "Cluj-Napoca", TODAY.plusDays(2), 4);
    private final Event java = event("e2", "Java workshop", "cluj-napoca", TODAY.plusDays(1), 10);
    private final Event cloud = event("e3", "Cloud day", "Brasov", TODAY.plusDays(3), 4);
    private final Event past = event("e4", "Past talk", "Cluj-Napoca", TODAY.minusDays(1), 2);

    private final TicketOffice office = new TicketOffice(List.of(react, java, cloud, past), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void onlyEventsWithFreeSeatsAreListedAsAvailable() {
        assertEquals(List.of(react, cloud, past), office.eventsWithFreeSeats());
    }

    @Test
    void bookedSeatsAreAddedUpPerEvent() {
        office.book(new BookingRequest("e1", "ana@example.com", 2));
        office.book(new BookingRequest("e1", "dan@example.com", 1));
        office.book(new BookingRequest("e3", "ana@example.com", 4));

        assertEquals(Map.of("e1", 3, "e3", 4), office.bookedSeatsPerEvent());
    }

    @Test
    void topEventsAreTheMostBookedAndTiesAreOrderedByName() {
        assertEquals(List.of(java, cloud, react), office.topEventsByBookedSeats(3));
    }

    @Test
    void upcomingEventsInACityAreSoonestFirstAndIgnoreTheCaseOfTheCity() {
        assertEquals(List.of(java, react), office.upcomingEventsIn("CLUJ-NAPOCA"));
    }
}
