package ro.frit.notification;

import ro.frit.notification.messages.BookedEventChanged;
import ro.frit.notification.messages.BookingCancelled;
import ro.frit.notification.messages.BookingConfirmed;
import ro.frit.notification.messages.BookingFact;

import java.time.format.DateTimeFormatter;

import static java.util.Locale.ENGLISH;

// What the customer reads, one sentence per fact. A switch over the sealed BookingFact: no default,
// so a fourth kind of message would not compile until it has a text.
final class NotificationTexts {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy 'at' HH:mm", ENGLISH);

    private NotificationTexts() {
    }

    static String textOf(BookingFact fact) {
        return switch (fact) {
            case BookingConfirmed confirmed -> "%d seat(s) for %s on %s are confirmed."
                    .formatted(confirmed.seats(), confirmed.eventName(), WHEN.format(confirmed.eventStartsAt()));
            case BookingCancelled(var _, var eventId, var _, var seats, var reason) -> switch (reason) {
                case BY_CUSTOMER -> "%d seat(s) for event %s are cancelled, as you asked.".formatted(seats, eventId);
                case EVENT_CANCELLED -> "%d seat(s) for event %s are cancelled: the event was called off.".formatted(seats, eventId);
            };
            case BookedEventChanged changed -> "Your event changed: %d seat(s) for %s, now at %s, %s, on %s."
                    .formatted(changed.seats(), changed.eventName(), changed.venueName(), changed.city(),
                            WHEN.format(changed.eventStartsAt()));
        };
    }
}
