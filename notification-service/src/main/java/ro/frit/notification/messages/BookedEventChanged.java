package ro.frit.notification.messages;

import static ro.frit.notification.messages.BookingFact.hasText;

import java.time.LocalDateTime;

public record BookedEventChanged(String bookingId, String eventId, String customerEmail, int seats, String eventName,
                                 String venueName, String city, LocalDateTime eventStartsAt) implements BookingFact {

    @Override
    public boolean isValid() {
        return hasText(bookingId) && hasText(customerEmail) && hasText(eventName) && hasText(venueName)
                && hasText(city) && eventStartsAt != null;
    }
}
