package ro.frit.ticketing.booking;

import ro.frit.ticketing.event.dto.EventResponse;

import java.time.LocalDateTime;

// A fact, in the past tense: what may cross the boundary is plain data (ids, values), never the Booking document.
// It carries the event's name and start time too, so the notification service can write to the customer without
// calling ticketing back.
public record BookingConfirmed(String bookingId, String eventId, String eventName, LocalDateTime eventStartsAt,
                               String customerEmail, int seats, LocalDateTime bookedAt) {

    static BookingConfirmed of(Booking booking, EventResponse event) {
        return new BookingConfirmed(booking.getId(), booking.getEventId(), event.name(), event.startsAt(),
                booking.getCustomerEmail(), booking.getSeats(), booking.getBookedAt());
    }
}
