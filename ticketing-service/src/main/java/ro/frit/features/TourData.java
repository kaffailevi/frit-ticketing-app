package ro.frit.features;

import ro.frit.ticketing.event.Event;
import ro.frit.ticketing.event.Venue;

import java.time.LocalDateTime;
import java.util.List;

// The same few events for every page of the Java tour.
final class TourData {

    private TourData() {
    }

    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 30, 12, 0);

    static List<Event> events() {
        return List.of(
                event("e1", "React meetup", "Cluj-Napoca", 2, 40, 31),
                event("e2", "Java workshop", "Cluj-Napoca", 1, 20, 20),
                event("e3", "Cloud day", "Brasov", 5, 100, 12),
                event("e4", "AI evening", "Iasi", 9, 60, 45));
    }

    private static Event event(String id, String name, String city, int daysAhead, int capacity, int booked) {
        return Event.builder()
                .id(id)
                .name(name)
                .venue(new Venue("Hall " + id, city))
                .startsAt(NOW.plusDays(daysAhead).withHour(18))
                .capacity(capacity)
                .bookedSeats(booked)
                .build();
    }
}
