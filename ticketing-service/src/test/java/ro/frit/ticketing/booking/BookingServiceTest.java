package ro.frit.ticketing.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.BookingSummaryResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.EventAlreadyStartedException;
import ro.frit.ticketing.event.EventFixtures;
import ro.frit.ticketing.event.EventNotFoundException;
import ro.frit.ticketing.event.EventRepository;
import ro.frit.ticketing.event.EventService;
import ro.frit.ticketing.event.NotEnoughSeatsException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ro.frit.ticketing.config.FixedClockConfiguration.TODAY_AT_NOON;
import static ro.frit.ticketing.event.EventFixtures.tomorrowEvening;

// The booking rows of docs/booking-cases.md, against a real MongoDB.
@MongoPersistenceTest
@Import({BookingService.class, EventService.class})
class BookingServiceTest {

    @MockitoBean // messaging is not what these tests are about; RabbitBookingEventPublisherTest covers it
    private BookingEventPublisher bookingEvents;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EventService eventService;

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

    @Test
    void theSummaryCountsTheBookingsAndAddsUpAndAveragesTheirSeats() {
        eventRepository.save(EventFixtures.event("big", "Iasi", TODAY_AT_NOON.plusDays(5), 100, 0));
        bookingService.book("big", new CreateBookingRequest("ana@example.com", 2));
        bookingService.book("big", new CreateBookingRequest("dan@example.com", 1));
        bookingService.book("big", new CreateBookingRequest("ioana@example.com", 3));
        bookingService.book("e1", new CreateBookingRequest("other@example.com", 1)); // another event: not counted

        assertEquals(new BookingSummaryResponse("big", 3, 6, 2.0), bookingService.summaryFor("big"));
    }

    @Test
    void anEventWithoutBookingsHasAnEmptySummary() {
        assertEquals(BookingSummaryResponse.empty("e1"), bookingService.summaryFor("e1"));
    }

    @Test
    void theSummaryOfAnUnknownEventIsNotFound() {
        assertThrows(EventNotFoundException.class, () -> bookingService.summaryFor("unknown"));
    }

    @Test
    void theBookingsOfAnEventAreListedOldestFirst() {
        BookingResponse first = bookingService.book("e1", new CreateBookingRequest("ana@example.com", 1));
        BookingResponse second = bookingService.book("e1", new CreateBookingRequest("dan@example.com", 1));

        assertEquals(List.of(first.id(), second.id()),
                bookingService.findAllFor("e1").stream().map(BookingResponse::id).toList());
    }

    @Test
    void aCancelledBookingIsGoneAndItsSeatsAreFreeAgain() {
        BookingResponse booking = bookingService.book("e1", new CreateBookingRequest("ana@example.com", 2));

        bookingService.cancel("e1", booking.id());

        assertEquals(0, bookingRepository.count());
        assertEquals(2, eventRepository.findById("e1").orElseThrow().availableSeats());
    }

    @Test
    void aBookingCannotBeCancelledThroughAnotherEvent() {
        BookingResponse booking = bookingService.book("e1", new CreateBookingRequest("ana@example.com", 1));

        assertThrows(BookingNotFoundException.class, () -> bookingService.cancel("e2", booking.id()));
        assertEquals(1, bookingRepository.count());
    }

    @Test
    void aBookingForAnEventThatHasStartedCannotBeCancelled() {
        eventRepository.save(EventFixtures.event("started", "Iasi", TODAY_AT_NOON.minusHours(1), 10, 1));
        bookingRepository.save(Booking.builder().id("b-old").eventId("started").customerEmail("ana@example.com")
                .seats(1).bookedAt(TODAY_AT_NOON.minusDays(2)).build());

        assertThrows(EventAlreadyStartedException.class, () -> bookingService.cancel("started", "b-old"));
        assertEquals(1, bookingRepository.count());
    }

    // Through Spring's application events: EventService publishes EventCancelled, BookingService listens.
    @Test
    void cancellingAnEventCancelsItsBookings() {
        bookingService.book("e1", new CreateBookingRequest("ana@example.com", 1));
        bookingService.book("e1", new CreateBookingRequest("dan@example.com", 1));

        eventService.cancel("e1");

        assertEquals(0, bookingRepository.count());
    }
}
