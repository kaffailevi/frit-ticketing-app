package ro.frit.ticketing.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

// An entity, like Event: equal by id.
@Getter
@ToString
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "id")
public class Booking {

    private final String id;
    private final String eventId;
    private final String customerEmail;
    private final int seats;
    private final LocalDateTime bookedAt;
}
