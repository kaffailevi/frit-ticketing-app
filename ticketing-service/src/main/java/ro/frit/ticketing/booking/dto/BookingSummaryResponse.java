package ro.frit.ticketing.booking.dto;

import lombok.experimental.FieldNameConstants;

// The bookings report of one event. averageSeatsPerBooking is null when there are no bookings yet.
@FieldNameConstants
public record BookingSummaryResponse(String eventId, long bookings, long seats, Double averageSeatsPerBooking) {

    public static BookingSummaryResponse empty(String eventId) {
        return new BookingSummaryResponse(eventId, 0, 0, null);
    }
}
