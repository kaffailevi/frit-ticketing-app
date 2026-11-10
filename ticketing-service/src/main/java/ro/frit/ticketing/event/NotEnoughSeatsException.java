package ro.frit.ticketing.event;

import ro.frit.ticketing.shared.error.ConflictException;

import java.util.Map;

public class NotEnoughSeatsException extends ConflictException {

    public NotEnoughSeatsException(String eventId, int availableSeats) {
        super("Event " + eventId + " has only " + availableSeats + " seat(s) left", Map.of("availableSeats", availableSeats));
    }
}
