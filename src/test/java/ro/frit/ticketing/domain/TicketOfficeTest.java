package ro.frit.ticketing.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

// One test per row of docs/booking-cases.md.
class TicketOfficeTest {

    private static final Instant NOW = Instant.parse("2026-11-30T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDateTime TODAY = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

    private static Event event(String id, LocalDateTime startsAt) {
        return Event.builder()
                .id(id)
                .name("Spring Boot workshop")
                .venue(new Venue("Hub", "Cluj-Napoca"))
                .startsAt(startsAt)
                .capacity(10)
                .bookedSeats(8)
                .build();
    }

    private static final Event TOMORROW_EVENING = event("e1", TODAY.plusDays(1).withHour(19));

    private static TicketOffice officeWith(Event... events) {
        return new TicketOffice(List.of(events), CLOCK);
    }

    private static int freeSeats(TicketOffice office, String eventId) {
        return office.findEvent(eventId).orElseThrow().availableSeats();
    }

    @Test
    void anUnknownEventIsReported() {
        BookingResult result = officeWith(TOMORROW_EVENING).book(new BookingRequest("unknown", "ana@example.com", 1));

        assertEquals(new BookingResult.UnknownEvent("unknown"), result);
    }

    @Test
    void aBookingWithEnoughFreeSeatsIsConfirmed() {
        TicketOffice office = officeWith(TOMORROW_EVENING);

        BookingResult result = office.book(new BookingRequest("e1", "ana@example.com", 1));

        BookingResult.Confirmed confirmed = assertInstanceOf(BookingResult.Confirmed.class, result);
        assertEquals(1, confirmed.booking().getSeats());
        assertEquals(1, freeSeats(office, "e1"));
    }

    @Test
    void theLastFreeSeatsCanBeBooked() {
        TicketOffice office = officeWith(TOMORROW_EVENING);

        BookingResult result = office.book(new BookingRequest("e1", "ana@example.com", 2));

        assertInstanceOf(BookingResult.Confirmed.class, result);
        assertEquals(0, freeSeats(office, "e1"));
    }

    @Test
    void askingForMoreSeatsThanAreFreeIsSoldOutAndChangesNothing() {
        TicketOffice office = officeWith(TOMORROW_EVENING);

        BookingResult result = office.book(new BookingRequest("e1", "ana@example.com", 3));

        assertEquals(new BookingResult.SoldOut("e1", 2), result);
        assertEquals(2, freeSeats(office, "e1"));
    }

    @Test
    void anEventThatHasStartedCannotBeBooked() {
        Event started = event("e2", TODAY.minusMinutes(1));
        TicketOffice office = officeWith(started);

        BookingResult result = office.book(new BookingRequest("e2", "ana@example.com", 1));

        assertEquals(new BookingResult.AlreadyStarted("e2", started.getStartsAt()), result);
        assertEquals(2, freeSeats(office, "e2"));
    }

    @Test
    void anEventStartingRightNowCannotBeBooked() {
        TicketOffice office = officeWith(event("e3", TODAY));

        BookingResult result = office.book(new BookingRequest("e3", "ana@example.com", 1));

        assertInstanceOf(BookingResult.AlreadyStarted.class, result);
    }

    @Test
    void twoEventsWithTheSameIdAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> officeWith(TOMORROW_EVENING, TOMORROW_EVENING));
    }
}
