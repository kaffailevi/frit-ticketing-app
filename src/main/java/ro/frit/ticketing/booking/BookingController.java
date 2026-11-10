package ro.frit.ticketing.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.BookingSummaryResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;

import java.net.URI;
import java.util.List;

// Cases: docs/booking-cases.md. 404 and 409 are exceptions from the services, answered by GlobalExceptionHandler.
@RestController
@RequestMapping("/events/{eventId}/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // springdoc sees the 201 from the code; the error answers come from GlobalExceptionHandler, so they are written here.
    @PostMapping
    @Operation(summary = "Book seats for an event")
    @ApiResponse(responseCode = "201", description = "Booked; the Location header points to the new booking")
    @ApiResponse(responseCode = "400", description = "Invalid body: no valid email, or seats not positive",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No such event",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Not enough seats (availableSeats), already started (startsAt), or lost a race",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<BookingResponse> book(@PathVariable String eventId, @Valid @RequestBody CreateBookingRequest body) {
        BookingResponse booking = bookingService.book(eventId, body);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(booking.id()).toUri();
        return ResponseEntity.created(location).body(booking);
    }

    @GetMapping
    public List<BookingResponse> bookings(@PathVariable String eventId) {
        return bookingService.findAllFor(eventId);
    }

    // After the first DELETE the booking is gone; repeating it answers 404 and changes nothing more.
    @DeleteMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String eventId, @PathVariable String bookingId) {
        bookingService.cancel(eventId, bookingId);
    }

    // "/summary" is more specific than "/{bookingId}", so Spring picks this method for it.
    @GetMapping("/summary")
    public BookingSummaryResponse summary(@PathVariable String eventId) {
        return bookingService.summaryFor(eventId);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse booking(@PathVariable String eventId, @PathVariable String bookingId) {
        return bookingService.findById(eventId, bookingId);
    }
}
