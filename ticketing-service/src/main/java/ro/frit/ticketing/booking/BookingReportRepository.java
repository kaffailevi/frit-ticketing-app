package ro.frit.ticketing.booking;

import ro.frit.ticketing.booking.dto.BookingSummaryResponse;

import java.util.Optional;

// A repository fragment for reports that MongoDB computes itself (aggregation pipelines).
public interface BookingReportRepository {

    // Empty when the event has no bookings.
    Optional<BookingSummaryResponse> summaryFor(String eventId);
}
