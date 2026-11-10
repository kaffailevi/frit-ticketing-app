package ro.frit.notification.messages;

import static ro.frit.notification.messages.BookingFact.hasText;

public record BookingCancelled(String bookingId, String eventId, String customerEmail, int seats, Reason reason)
        implements BookingFact {

    public enum Reason { BY_CUSTOMER, EVENT_CANCELLED }

    @Override
    public boolean isValid() {
        return hasText(bookingId) && hasText(eventId) && hasText(customerEmail) && seats > 0 && reason != null;
    }
}
