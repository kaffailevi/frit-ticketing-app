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
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
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
// The search filters on the city and sorts by start time; one index serves both.
@CompoundIndex(name = "city_startsAt", def = "{'venue.city': 1, 'startsAt': 1}")
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

    // Optimistic locking: MongoDB saves the event only if its stored version is still the one that was read,
    // and Spring Data increases it on every save. If someone else saved in between (booked seats at the same
    // moment), the save throws OptimisticLockingFailureException instead of overwriting their booking.
    // null = never saved. @With: Spring Data sets the new version on this immutable object through withVersion().
    @Version
    @With
    private final Long version;

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
