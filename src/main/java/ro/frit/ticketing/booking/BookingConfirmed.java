package ro.frit.ticketing.booking;

import java.time.LocalDateTime;

// A fact, in the past tense: what may cross the boundary is plain data (ids, values), never the Booking document.
public record BookingConfirmed(String bookingId, String eventId, String customerEmail, int seats, LocalDateTime bookedAt) {

    static BookingConfirmed of(Booking booking) {
        return new BookingConfirmed(booking.getId(), booking.getEventId(), booking.getCustomerEmail(),
                booking.getSeats(), booking.getBookedAt());
    }
}
