package ro.frit.ticketing.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// One document in the "events" collection, with the venue embedded in it (it is always read together with the event).
// An entity: equal by id. Immutable: Spring Data creates it through the all-arguments constructor,
// and booking seats produces a new Event. The constraints are checked before every save (MongoConfig).
@Getter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@FieldNameConstants
@Document(collection = "events")
public class Event {

    @Id
    private final String id;

    @NotBlank
    private final String name;

    @NotNull
    @Valid
    private final Venue venue;

    @NotNull
    private final LocalDateTime startsAt;

    @Positive
    private final int capacity;

    @PositiveOrZero
    private final int bookedSeats;

    public int availableSeats() {
        return capacity - bookedSeats;
    }

    public boolean hasStartedAt(LocalDateTime moment) {
        return !startsAt.isAfter(moment);
    }

    public Event withSeatsBooked(int seats) {
        return toBuilder().bookedSeats(bookedSeats + seats).build();
    }
}
