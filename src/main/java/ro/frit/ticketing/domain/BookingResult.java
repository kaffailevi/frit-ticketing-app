package ro.frit.ticketing.domain;

import java.time.LocalDateTime;

// Every possible outcome of a booking, as a closed set of types (sealed).
// A switch over BookingResult needs no default: the compiler knows all four cases,
// and it reports every switch that forgets one when a fifth outcome is added.
public sealed interface BookingResult {

    record Confirmed(Booking booking) implements BookingResult {
    }

    record SoldOut(String eventId, int availableSeats) implements BookingResult {
    }

    record UnknownEvent(String eventId) implements BookingResult {
    }

    record AlreadyStarted(String eventId, LocalDateTime startsAt) implements BookingResult {
    }

    // A record pattern takes each result apart right in the case label.
    default String describe() {
        return switch (this) {
            case Confirmed(Booking booking) ->
                    "Confirmed: %d seat(s) for event %s".formatted(booking.getSeats(), booking.getEventId());
            case SoldOut(String eventId, int availableSeats) ->
                    "Sold out: event %s has only %d seat(s) left".formatted(eventId, availableSeats);
            case UnknownEvent(String eventId) -> "Unknown event: %s".formatted(eventId);
            case AlreadyStarted(String eventId, LocalDateTime startsAt) ->
                    "Too late: event %s started at %s".formatted(eventId, startsAt);
        };
    }
}
