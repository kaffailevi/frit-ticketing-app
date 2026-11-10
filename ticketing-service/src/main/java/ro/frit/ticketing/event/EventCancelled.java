package ro.frit.ticketing.event;

import java.time.LocalDateTime;

// An application event: the event was cancelled (deleted). Its bookings are BookingService's business.
public record EventCancelled(String eventId, String name, LocalDateTime startsAt) {

    static EventCancelled of(Event event) {
        return new EventCancelled(event.getId(), event.getName(), event.getStartsAt());
    }
}
