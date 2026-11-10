package ro.frit.ticketing.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;

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

    public Optional<Booking> findBooking(String id) {
        return bookings.stream().filter(booking -> booking.getId().equals(id)).findFirst();
    }

    public List<Event> allEvents() {
        return List.copyOf(events.values());
    }

    public Event addEvent(Event event) {
        if (events.putIfAbsent(event.getId(), event) != null) {
            throw new IllegalArgumentException("Two events have the id " + event.getId());
        }
        return event;
    }

    // The queries below are streams: describe what you want, not the loop that finds it.

    public List<Event> eventsWithFreeSeats() {
        return events.values().stream()
                .filter(event -> event.availableSeats() > 0)
                .toList();
    }

    public Map<String, Integer> bookedSeatsPerEvent() {
        return bookings.stream()
                .collect(Collectors.groupingBy(Booking::getEventId, Collectors.summingInt(Booking::getSeats)));
    }

    // Most booked first; on a tie, alphabetical by name, so the order is always the same.
    public List<Event> topEventsByBookedSeats(int limit) {
        return events.values().stream()
                .sorted(Comparator.comparingInt(Event::getBookedSeats).reversed().thenComparing(Event::getName))
                .limit(limit)
                .toList();
    }

    // Java 21 + 22: a switch over the sealed result. The three outcomes without seats share one case,
    // and '_' says their data is not needed.
    public int bookAll(List<BookingRequest> requests) {
        return requests.stream()
                .map(this::book)
                .mapToInt(result -> switch (result) {
                    case BookingResult.Confirmed(Booking booking) -> booking.getSeats();
                    case BookingResult.SoldOut _, BookingResult.UnknownEvent _, BookingResult.AlreadyStarted _ -> 0;
                })
                .sum();
    }

    // Java 21: every ordered collection has getFirst() and getLast().
    public Optional<Booking> lastBooking() {
        return bookings.isEmpty() ? Optional.empty() : Optional.of(bookings.getLast());
    }

    // Java 24: a gatherer is a custom step in the middle of a stream; windowFixed cuts it into groups.
    public List<List<Booking>> bookingsInBatches(int size) {
        return bookings.stream().gather(Gatherers.windowFixed(size)).toList();
    }

    public List<Event> upcomingEventsIn(String city) {
        LocalDateTime now = LocalDateTime.now(clock);
        return events.values().stream()
                .filter(event -> event.getVenue().city().equalsIgnoreCase(city))
                .filter(event -> !event.hasStartedAt(now))
                .sorted(Comparator.comparing(Event::getStartsAt))
                .toList();
    }
}
