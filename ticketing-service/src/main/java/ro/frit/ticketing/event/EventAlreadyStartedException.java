package ro.frit.ticketing.event;

import ro.frit.ticketing.shared.error.ConflictException;

import java.time.LocalDateTime;
import java.util.Map;

public class EventAlreadyStartedException extends ConflictException {

    public EventAlreadyStartedException(String eventId, LocalDateTime startsAt) {
        super("Event " + eventId + " has already started", Map.of("startsAt", startsAt));
    }
}
