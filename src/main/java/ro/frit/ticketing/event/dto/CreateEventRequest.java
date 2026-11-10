package ro.frit.ticketing.event.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ro.frit.ticketing.event.Event;
import ro.frit.ticketing.event.Venue;

import java.time.LocalDateTime;

// The body of POST /events. Bean Validation checks it before the controller method runs;
// an invalid body is answered with 400 and never reaches the service.
public record CreateEventRequest(@NotBlank String name,
                                 @NotBlank String venueName,
                                 @NotBlank String city,
                                 @NotNull @Future LocalDateTime startsAt,
                                 @NotNull @Positive Integer capacity) {

    public Event toEvent(String id) {
        return Event.builder()
                .id(id)
                .name(name)
                .venue(new Venue(venueName, city))
                .startsAt(startsAt)
                .capacity(capacity)
                .bookedSeats(0)
                .build();
    }
}
