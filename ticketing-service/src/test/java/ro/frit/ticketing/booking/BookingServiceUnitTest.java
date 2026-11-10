package ro.frit.ticketing.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ro.frit.ticketing.event.dto.EventResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.EventCancelled;
import ro.frit.ticketing.event.EventChanged;
import ro.frit.ticketing.event.EventService;
import ro.frit.ticketing.event.NotEnoughSeatsException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ro.frit.ticketing.config.FixedClockConfiguration.NOW;
import static ro.frit.ticketing.config.FixedClockConfiguration.TODAY_AT_NOON;

// What BookingService tells the outside world through its port. No Spring, no database: the port is a mock,
// and so are the repository and EventService. This is what a port buys: the service is testable on its own.
class BookingServiceUnitTest {

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final EventService eventService = mock(EventService.class);
    private final BookingEventPublisher bookingEvents = mock(BookingEventPublisher.class);
    private final BookingService bookingService = new BookingService(
            bookingRepository, eventService, Clock.fixed(NOW, java.time.ZoneOffset.UTC), bookingEvents);

    private static Booking booking(String id, String email, int seats) {
        return Booking.builder().id(id).eventId("e1").customerEmail(email).seats(seats).bookedAt(TODAY_AT_NOON).build();
    }

    @BeforeEach
    void theRepositoryReturnsWhatItStores() {
        when(bookingRepository.insert(any(Booking.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void aConfirmedBookingIsAnnouncedWithTheEventsNameAndStartTime() {
        LocalDateTime startsAt = LocalDateTime.of(2027, 2, 10, 18, 0);
        when(eventService.reserveSeats("e1", 2)).thenReturn(
                new EventResponse("e1", "React Workshop", "Hub", "Cluj-Napoca", startsAt, 30, 28));

        bookingService.book("e1", new CreateBookingRequest("ana@example.com", 2));

        ArgumentCaptor<BookingConfirmed> announced = ArgumentCaptor.forClass(BookingConfirmed.class);
        verify(bookingEvents).publish(announced.capture());
        assertEquals("React Workshop", announced.getValue().eventName());
        assertEquals(startsAt, announced.getValue().eventStartsAt());
        assertEquals("ana@example.com", announced.getValue().customerEmail());
    }

    @Test
    void aRefusedBookingAnnouncesNothing() {
        doThrow(new NotEnoughSeatsException("e1", 1)).when(eventService).reserveSeats("e1", 2);

        assertThrows(NotEnoughSeatsException.class, () -> bookingService.book("e1", new CreateBookingRequest("ana@example.com", 2)));
        verifyNoInteractions(bookingEvents);
    }

    @Test
    void aBookingCancelledByTheCustomerIsAnnouncedWithThatReason() {
        Booking booking = booking("b1", "ana@example.com", 2);
        when(bookingRepository.findByIdAndEventId("b1", "e1")).thenReturn(Optional.of(booking));

        bookingService.cancel("e1", "b1");

        verify(bookingEvents).publish(BookingCancelled.of(booking, BookingCancelled.Reason.BY_CUSTOMER));
    }

    @Test
    void everyCustomerOfACancelledEventIsTold() {
        Booking ana = booking("b1", "ana@example.com", 1);
        Booking dan = booking("b2", "dan@example.com", 3);
        when(bookingRepository.findByEventIdOrderByBookedAt("e1")).thenReturn(List.of(ana, dan));

        bookingService.onEventCancelled(new EventCancelled("e1", "React Workshop", TODAY_AT_NOON.plusDays(1)));

        verify(bookingRepository).deleteAll(List.of(ana, dan));
        verify(bookingEvents).publish(BookingCancelled.of(ana, BookingCancelled.Reason.EVENT_CANCELLED));
        verify(bookingEvents).publish(BookingCancelled.of(dan, BookingCancelled.Reason.EVENT_CANCELLED));
    }

    @Test
    void everyCustomerOfAChangedEventLearnsTheNewDetails() {
        Booking ana = booking("b1", "ana@example.com", 1);
        when(bookingRepository.findByEventIdOrderByBookedAt("e1")).thenReturn(List.of(ana));
        EventChanged changed = new EventChanged("e1", "React Workshop", "New Hall", "Iasi", LocalDateTime.of(2027, 2, 11, 18, 0));

        bookingService.onEventChanged(changed);

        verify(bookingEvents).publish(BookedEventChanged.of(ana, changed));
    }
}
