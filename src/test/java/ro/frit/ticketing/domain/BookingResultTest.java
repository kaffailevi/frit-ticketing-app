package ro.frit.ticketing.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingResultTest {

    @Test
    void aConfirmationSaysHowManySeatsForWhichEvent() {
        Booking booking = Booking.builder().id("b1").eventId("e1").customerEmail("ana@example.com").seats(2).build();

        assertEquals("Confirmed: 2 seat(s) for event e1", new BookingResult.Confirmed(booking).describe());
    }

    @Test
    void aSoldOutResultSaysHowManySeatsAreLeft() {
        assertEquals("Sold out: event e1 has only 1 seat(s) left", new BookingResult.SoldOut("e1", 1).describe());
    }

    @Test
    void anUnknownEventResultNamesTheEvent() {
        assertEquals("Unknown event: e9", new BookingResult.UnknownEvent("e9").describe());
    }

    @Test
    void anAlreadyStartedResultSaysWhenTheEventStarted() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 30, 19, 0);

        assertEquals("Too late: event e1 started at 2026-11-30T19:00",
                new BookingResult.AlreadyStarted("e1", start).describe());
    }
}
