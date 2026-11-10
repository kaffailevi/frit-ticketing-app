package ro.frit.ticketing.event;

import java.time.LocalDateTime;

// An application event: published inside this service when something its customers care about changes
// (name, place, time). EventService does not know who listens; BookingService does.
public record EventChanged(String eventId, String name, String venueName, String city, LocalDateTime startsAt) {

    static EventChanged of(Event event) {
        return new EventChanged(event.getId(), event.getName(), event.getVenue().name(), event.getVenue().city(),
                event.getStartsAt());
    }
}
