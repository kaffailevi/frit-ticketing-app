package ro.frit.notification.messages;

import static ro.frit.notification.messages.BookingFact.hasText;

import java.time.LocalDateTime;

// This service's own copy of the contract: the fields it reads from ticketing's message, nothing shared at compile
// time. A field ticketing adds later is ignored; a field this service needs and does not get makes the message invalid.
public record BookingConfirmed(String bookingId, String eventId, String eventName, LocalDateTime eventStartsAt,
                               String customerEmail, int seats, LocalDateTime bookedAt) implements BookingFact {

    @Override
    public boolean isValid() {
        return hasText(bookingId) && hasText(eventId) && hasText(eventName) && hasText(customerEmail)
                && seats > 0 && eventStartsAt != null;
    }
}
