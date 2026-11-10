package ro.frit.ticketing.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.BookingSummaryResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.EventNotFoundException;
import ro.frit.ticketing.event.EventService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

// Books seats. The rules about the event itself (does it exist, has it started, are there enough seats)
// belong to EventService; this service asks it to reserve the seats, then records who booked them.
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventService eventService;
    private final Clock clock;

    public BookingResponse book(String eventId, CreateBookingRequest request) {
        eventService.reserveSeats(eventId, request.seats());
        Booking booking = Booking.builder()
                .id(UUID.randomUUID().toString())
                .eventId(eventId)
                .customerEmail(request.customerEmail())
                .seats(request.seats())
                .bookedAt(LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS)) // MongoDB keeps milliseconds
                .build();
        return BookingResponse.from(bookingRepository.insert(booking));
    }

    // The booking must belong to the event in the path; otherwise any booking could be read through any event's URL.
    public BookingResponse findById(String eventId, String bookingId) {
        return BookingResponse.from(getBooking(eventId, bookingId));
    }

    public List<BookingResponse> findAllFor(String eventId) {
        ensureEventExists(eventId);
        return bookingRepository.findByEventIdOrderByBookedAt(eventId).stream().map(BookingResponse::from).toList();
    }

    // The seats go back to the event first: if the event has started, that throws and the booking stays.
    public void cancel(String eventId, String bookingId) {
        Booking booking = getBooking(eventId, bookingId);
        eventService.releaseSeats(eventId, booking.getSeats());
        bookingRepository.delete(booking);
    }

    // An event without bookings has an empty report; an unknown event is a 404.
    public BookingSummaryResponse summaryFor(String eventId) {
        ensureEventExists(eventId);
        return bookingRepository.summaryFor(eventId).orElseGet(() -> BookingSummaryResponse.empty(eventId));
    }

    private Booking getBooking(String eventId, String bookingId) {
        return bookingRepository.findByIdAndEventId(bookingId, eventId)
                .orElseThrow(() -> new BookingNotFoundException(eventId, bookingId));
    }

    private void ensureEventExists(String eventId) {
        if (!eventService.exists(eventId)) {
            throw new EventNotFoundException(eventId);
        }
    }
}
