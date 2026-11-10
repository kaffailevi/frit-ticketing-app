package ro.frit.ticketing.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// The body of POST /events/{eventId}/bookings. The event id comes from the path, never from the body.
// seats is an Integer so that a missing field is null (refused by @NotNull) instead of a silent 0.
public record CreateBookingRequest(@NotBlank @Email String customerEmail, @NotNull @Positive Integer seats) {
}
