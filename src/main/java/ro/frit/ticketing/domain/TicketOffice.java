package ro.frit.ticketing.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Sells seats for a fixed list of events, in memory. Session 1: no framework, no database.
// The clock is passed in, so a test can decide what "now" is (see docs/booking-cases.md).
public class TicketOffice {

    private final Map<String, Event> events = new LinkedHashMap<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final Clock clock;

    public TicketOffice(List<Event> events, Clock clock) {
        this.clock = clock;
        for (Event event : events) {
            if (this.events.putIfAbsent(event.getId(), event) != null) {
                throw new IllegalArgumentException("Two events have the id " + event.getId());
            }
        }
    }

    public BookingResult book(BookingRequest request) {
        Event event = events.get(request.eventId());
        if (event == null) {
            return new BookingResult.UnknownEvent(request.eventId());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (event.hasStartedAt(now)) {
            return new BookingResult.AlreadyStarted(event.getId(), event.getStartsAt());
        }
        if (request.seats() > event.availableSeats()) {
            return new BookingResult.SoldOut(event.getId(), event.availableSeats());
        }

        events.put(event.getId(), event.withSeatsBooked(request.seats()));
        Booking booking = Booking.builder()
                .id(UUID.randomUUID().toString())
                .eventId(event.getId())
                .customerEmail(request.customerEmail())
                .seats(request.seats())
                .bookedAt(now)
                .build();
        bookings.add(booking);
        return new BookingResult.Confirmed(booking);
    }

    public Optional<Event> findEvent(String id) {
        return Optional.ofNullable(events.get(id));
    }
}
