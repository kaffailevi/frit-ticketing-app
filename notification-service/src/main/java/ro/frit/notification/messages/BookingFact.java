package ro.frit.notification.messages;

// Every message this service accepts from ticketing. A sealed type: the text of a notification is a switch over
// exactly these three, checked by the compiler (NotificationTexts).
public sealed interface BookingFact permits BookingConfirmed, BookingCancelled, BookedEventChanged {

    String bookingId();

    String customerEmail();

    // Messages come from outside: anything missing that a notification needs makes the message invalid.
    boolean isValid();

    static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
