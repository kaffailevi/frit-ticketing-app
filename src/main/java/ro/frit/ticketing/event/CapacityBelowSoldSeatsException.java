package ro.frit.ticketing.event;

import ro.frit.ticketing.shared.error.ConflictException;

import java.util.Map;

public class CapacityBelowSoldSeatsException extends ConflictException {

    public CapacityBelowSoldSeatsException(String eventId, int bookedSeats) {
        super("Event " + eventId + " has already sold " + bookedSeats + " seat(s); the capacity cannot be lower",
                Map.of("bookedSeats", bookedSeats));
    }
}
