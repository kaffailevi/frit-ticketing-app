package ro.frit.ticketing.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.frit.ticketing.domain.BookingResult;
import ro.frit.ticketing.domain.TicketOffice;

import java.net.URI;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

// Cases: docs/booking-cases.md (HTTP section).
@RestController
@RequestMapping("/events/{eventId}/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final TicketOffice ticketOffice;

    // Every outcome of the sealed BookingResult becomes an HTTP answer; the compiler checks none is forgotten.
    @PostMapping
    public ResponseEntity<?> book(@PathVariable String eventId, @Valid @RequestBody BookingBody body) {
        return switch (ticketOffice.book(body.toRequest(eventId))) {
            case BookingResult.Confirmed(var booking) -> {
                URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}").buildAndExpand(booking.getId()).toUri();
                yield ResponseEntity.created(location).body(BookingResponse.from(booking));
            }
            case BookingResult.UnknownEvent(var id) -> problem(NOT_FOUND, "No event with id " + id);
            case BookingResult.SoldOut(var id, var availableSeats) -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(CONFLICT,
                        "Event " + id + " has only " + availableSeats + " seat(s) left");
                problem.setProperty("availableSeats", availableSeats);
                yield ResponseEntity.of(problem).build();
            }
            case BookingResult.AlreadyStarted(var id, var startsAt) -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(CONFLICT, "Event " + id + " has already started");
                problem.setProperty("startsAt", startsAt);
                yield ResponseEntity.of(problem).build();
            }
        };
    }

    // The booking must belong to the event in the path; otherwise any booking could be read through any event's URL.
    @GetMapping("/{bookingId}")
    public ResponseEntity<?> booking(@PathVariable String eventId, @PathVariable String bookingId) {
        return ticketOffice.findBooking(bookingId)
                .filter(booking -> booking.getEventId().equals(eventId))
                .<ResponseEntity<?>>map(booking -> ResponseEntity.ok(BookingResponse.from(booking)))
                .orElseGet(() -> problem(NOT_FOUND, "No booking with id " + bookingId + " for event " + eventId));
    }

    private static ResponseEntity<?> problem(HttpStatus status, String detail) {
        return ResponseEntity.of(ProblemDetail.forStatusAndDetail(status, detail)).build();
    }
}
