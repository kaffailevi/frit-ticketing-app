package ro.frit.ticketing.booking;

import ro.frit.ticketing.event.EventChanged;

import java.time.LocalDateTime;

// What one customer must learn when the event they booked changes: their booking, and the event as it is now.
public record BookedEventChanged(String bookingId, String eventId, String customerEmail, int seats,
                                 String eventName, String venueName, String city, LocalDateTime eventStartsAt) {

    static BookedEventChanged of(Booking booking, EventChanged changed) {
        return new BookedEventChanged(booking.getId(), booking.getEventId(), booking.getCustomerEmail(), booking.getSeats(),
                changed.name(), changed.venueName(), changed.city(), changed.startsAt());
    }
}
