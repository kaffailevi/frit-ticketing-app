package ro.frit.ticketing.event;

import java.time.LocalDateTime;

import static ro.frit.ticketing.config.FixedClockConfiguration.TODAY_AT_NOON;

// Events for the tests, relative to the fixed "now" of FixedClockConfiguration.
public final class EventFixtures {

    private EventFixtures() {
    }

    public static Event event(String id, String city, LocalDateTime startsAt, int capacity, int bookedSeats) {
        return Event.builder()
                .id(id)
                .name("Event " + id)
                .venue(new Venue("Venue " + id, city))
                .startsAt(startsAt)
                .capacity(capacity)
                .bookedSeats(bookedSeats)
                .build();
    }

    // Capacity 10, 8 booked (2 free), tomorrow at 19:00 — the event of docs/booking-cases.md.
    public static Event tomorrowEvening(String id) {
        return event(id, "Cluj-Napoca", TODAY_AT_NOON.plusDays(1).withHour(19), 10, 8);
    }
}
