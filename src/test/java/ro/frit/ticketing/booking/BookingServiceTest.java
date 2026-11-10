package ro.frit.ticketing.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.EventNotFoundException;
import ro.frit.ticketing.event.EventRepository;
import ro.frit.ticketing.event.EventService;
import ro.frit.ticketing.event.NotEnoughSeatsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ro.frit.ticketing.config.FixedClockConfiguration.TODAY_AT_NOON;
import static ro.frit.ticketing.event.EventFixtures.tomorrowEvening;

// The booking rows of docs/booking-cases.md, against a real MongoDB.
@MongoPersistenceTest
@Import({BookingService.class, EventService.class})
class BookingServiceTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    @BeforeEach
    void emptyCollections() {
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        eventRepository.save(tomorrowEvening("e1"));
    }

    @Test
    void aConfirmedBookingIsStoredWithWhoBookedAndWhen() {
        BookingResponse booking = bookingService.book("e1", new CreateBookingRequest("ana@example.com", 2));

        assertEquals(booking, bookingService.findById("e1", booking.id()));
        assertEquals("ana@example.com", booking.customerEmail());
        assertEquals(TODAY_AT_NOON, booking.bookedAt());
    }

    @Test
    void aConfirmedBookingTakesItsSeatsFromTheEvent() {
        bookingService.book("e1", new CreateBookingRequest("ana@example.com", 2));

        assertEquals(0, eventRepository.findById("e1").orElseThrow().availableSeats());
    }

    @Test
    void aBookingForAnUnknownEventStoresNothing() {
        assertThrows(EventNotFoundException.class,
                () -> bookingService.book("unknown", new CreateBookingRequest("ana@example.com", 1)));
        assertEquals(0, bookingRepository.count());
    }

    @Test
    void aSoldOutBookingStoresNothing() {
        assertThrows(NotEnoughSeatsException.class,
                () -> bookingService.book("e1", new CreateBookingRequest("ana@example.com", 3)));
        assertEquals(0, bookingRepository.count());
    }

    @Test
    void aBookingIsNotFoundThroughAnotherEvent() {
        BookingResponse booking = bookingService.book("e1", new CreateBookingRequest("ana@example.com", 1));

        assertThrows(BookingNotFoundException.class, () -> bookingService.findById("e2", booking.id()));
    }
}
