package ro.frit.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import ro.frit.notification.messages.BookedEventChanged;
import ro.frit.notification.messages.BookingCancelled;
import ro.frit.notification.messages.BookingConfirmed;
import ro.frit.notification.messages.BookingFact;

import static ro.frit.notification.config.MessagingConfig.BOOKED_EVENT_CHANGED_QUEUE;
import static ro.frit.notification.config.MessagingConfig.BOOKING_CANCELLED_QUEUE;
import static ro.frit.notification.config.MessagingConfig.BOOKING_CONFIRMED_QUEUE;

// Receives ticketing's facts from this service's queues. The JSON becomes the parameter's type.
// An invalid message is rejected without requeue: RabbitMQ moves it to the queue's dead-letter queue.
@Component
@RequiredArgsConstructor
public class BookingEventsListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = BOOKING_CONFIRMED_QUEUE)
    public void onBookingConfirmed(BookingConfirmed confirmed, @Header(AmqpHeaders.MESSAGE_ID) String messageId) {
        handle(confirmed, messageId);
    }

    @RabbitListener(queues = BOOKING_CANCELLED_QUEUE)
    public void onBookingCancelled(BookingCancelled cancelled, @Header(AmqpHeaders.MESSAGE_ID) String messageId) {
        handle(cancelled, messageId);
    }

    @RabbitListener(queues = BOOKED_EVENT_CHANGED_QUEUE)
    public void onBookedEventChanged(BookedEventChanged changed, @Header(AmqpHeaders.MESSAGE_ID) String messageId) {
        handle(changed, messageId);
    }

    private void handle(BookingFact fact, String messageId) {
        if (!fact.isValid()) {
            throw new AmqpRejectAndDontRequeueException("Invalid message " + messageId + ": " + fact);
        }
        notificationService.notify(messageId, fact);
    }
}
