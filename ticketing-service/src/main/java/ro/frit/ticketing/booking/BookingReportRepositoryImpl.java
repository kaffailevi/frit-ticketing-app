package ro.frit.ticketing.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import ro.frit.ticketing.booking.dto.BookingSummaryResponse;

import java.util.Optional;

import static ro.frit.ticketing.booking.dto.BookingSummaryResponse.Fields.averageSeatsPerBooking;
import static ro.frit.ticketing.booking.dto.BookingSummaryResponse.Fields.bookings;
import static ro.frit.ticketing.booking.dto.BookingSummaryResponse.Fields.seats;

// The report is computed inside MongoDB by an aggregation pipeline, so only one small document comes back,
// however many bookings there are:
//   $match   the bookings of this event
//   $group   by eventId: count them, sum and average their seats
//   $project rename the group's _id to eventId, so the result maps straight onto BookingSummaryResponse
@RequiredArgsConstructor
public class BookingReportRepositoryImpl implements BookingReportRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<BookingSummaryResponse> summaryFor(String eventId) {
        Aggregation pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where(Booking.Fields.eventId).is(eventId)),
                Aggregation.group(Booking.Fields.eventId)
                        .count().as(bookings)
                        .sum(Booking.Fields.seats).as(seats)
                        .avg(Booking.Fields.seats).as(averageSeatsPerBooking),
                Aggregation.project(bookings, seats, averageSeatsPerBooking)
                        .and(BookingSummaryResponse.Fields.eventId).previousOperation());

        return Optional.ofNullable(
                mongoTemplate.aggregate(pipeline, Booking.class, BookingSummaryResponse.class).getUniqueMappedResult());
    }
}
