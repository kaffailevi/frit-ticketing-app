package ro.frit.ticketing.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketOfficeModernJavaTest {

    private static final Instant NOW = Instant.parse("2026-11-30T12:00:00Z");

    private final TicketOffice office = new TicketOffice(List.of(Event.builder()
            .id("e1")
            .name("Java workshop")
            .venue(new Venue("Hub", "Cluj-Napoca"))
            .startsAt(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC).plusDays(1))
            .capacity(5)
            .bookedSeats(0)
            .build()), Clock.fixed(NOW, ZoneOffset.UTC));

    private static BookingRequest request(String eventId, String email, int seats) {
        return new BookingRequest(eventId, email, seats);
    }

    @Test
    void bookingSeveralRequestsCountsOnlyTheConfirmedSeats() {
        int booked = office.bookAll(List.of(
                request("e1", "ana@example.com", 2),
                request("unknown", "dan@example.com", 3),
                request("e1", "ioana@example.com", 9),
                request("e1", "mihai@example.com", 1)));

        assertEquals(3, booked);
    }

    @Test
    void theLastBookingIsTheMostRecentConfirmedOne() {
        office.book(request("e1", "ana@example.com", 1));
        office.book(request("e1", "dan@example.com", 1));

        assertEquals("dan@example.com", office.lastBooking().orElseThrow().getCustomerEmail());
    }

    @Test
    void thereIsNoLastBookingBeforeTheFirstOne() {
        assertTrue(office.lastBooking().isEmpty());
    }

    @Test
    void bookingsAreGroupedInBatchesOfTheGivenSize() {
        for (String email : List.of("a@example.com", "b@example.com", "c@example.com")) {
            office.book(request("e1", email, 1));
        }

        List<Integer> batchSizes = office.bookingsInBatches(2).stream().map(List::size).toList();

        assertEquals(List.of(2, 1), batchSizes);
    }
}
