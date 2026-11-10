package ro.frit.ticketing.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// One document in the "bookings" collection. It points to its event by eventId instead of embedding it:
// an event has many bookings, and the event changes (booked seats) while the bookings do not.
@Getter
@ToString
@Builder
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Document(collection = "bookings")
public class Booking {

    @Id
    private final String id;

    @NotBlank
    private final String eventId;

    @NotBlank
    @Email
    private final String customerEmail;

    @Positive
    private final int seats;

    @NotNull
    private final LocalDateTime bookedAt;
}
