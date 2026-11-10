package ro.frit.ticketing.event.dto;

import ro.frit.ticketing.event.Event;

import java.time.LocalDateTime;

// What the API shows of an event. The document is never serialized directly,
// so the JSON stays the same when the storage changes.
public record EventResponse(String id, String name, String venueName, String city, LocalDateTime startsAt,
                            int capacity, int availableSeats) {

    public static EventResponse from(Event event) {
        return new EventResponse(event.getId(), event.getName(), event.getVenue().name(), event.getVenue().city(),
                event.getStartsAt(), event.getCapacity(), event.availableSeats());
    }
}
