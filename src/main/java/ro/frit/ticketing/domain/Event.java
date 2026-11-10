package ro.frit.ticketing.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

// An entity: it stays the same event for its whole life, whatever changes, so equality is by id only.
// No setters: booking seats produces a new Event with more booked seats.
@Getter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "id")
public class Event {

    private final String id;
    private final String name;
    private final Venue venue;
    private final LocalDateTime startsAt;
    private final int capacity;
    private final int bookedSeats;

    public int availableSeats() {
        return capacity - bookedSeats;
    }

    public boolean hasStartedAt(LocalDateTime moment) {
        return !startsAt.isAfter(moment);
    }

    public Event withSeatsBooked(int seats) {
        return toBuilder().bookedSeats(bookedSeats + seats).build();
    }
}
