package ro.frit.ticketing.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ro.frit.ticketing.domain.BookingRequest;

// The body of POST /events/{eventId}/bookings. The event id comes from the path, never from the body.
// seats is an Integer so that a missing field is null (refused by @NotNull) instead of a silent 0.
public record BookingBody(@NotBlank @Email String customerEmail, @NotNull @Positive Integer seats) {

    public BookingRequest toRequest(String eventId) {
        return new BookingRequest(eventId, customerEmail, seats);
    }
}
