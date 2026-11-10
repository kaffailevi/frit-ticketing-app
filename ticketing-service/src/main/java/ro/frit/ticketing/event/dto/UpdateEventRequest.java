package ro.frit.ticketing.event.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

// The body of PUT /events/{id}: the whole new state of the details (PUT replaces, it does not patch).
public record UpdateEventRequest(@NotBlank String name,
                                 @NotBlank String venueName,
                                 @NotBlank String city,
                                 @NotNull @Future LocalDateTime startsAt,
                                 @NotNull @Positive Integer capacity) {
}
