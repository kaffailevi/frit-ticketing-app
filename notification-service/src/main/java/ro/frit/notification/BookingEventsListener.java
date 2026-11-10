package ro.frit.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventsListener {

    private final NotificationService notificationService;

    // The correlation id of the booking request arrives as an optional header; a message without one is still handled.
    @RabbitListener(queues = BOOKING_CONFIRMED_QUEUE)
    public void onBookingConfirmed(BookingConfirmed confirmed, @Header(AmqpHeaders.MESSAGE_ID) String messageId,
                                   @Header(name = CorrelationIdFilter.HEADER, required = false) String correlationId) {
        handle(confirmed, messageId, correlationId);
    }

    @RabbitListener(queues = BOOKING_CANCELLED_QUEUE)
    public void onBookingCancelled(BookingCancelled cancelled, @Header(AmqpHeaders.MESSAGE_ID) String messageId,
                                   @Header(name = CorrelationIdFilter.HEADER, required = false) String correlationId) {
        handle(cancelled, messageId, correlationId);
    }

    @RabbitListener(queues = BOOKED_EVENT_CHANGED_QUEUE)
    public void onBookedEventChanged(BookedEventChanged changed, @Header(AmqpHeaders.MESSAGE_ID) String messageId,
                                     @Header(name = CorrelationIdFilter.HEADER, required = false) String correlationId) {
        handle(changed, messageId, correlationId);
    }

    private void handle(BookingFact fact, String messageId, String correlationId) {
        if (correlationId != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);
        }
        try {
            log.info("Received {} for booking {}", fact.getClass().getSimpleName(), fact.bookingId());
            if (!fact.isValid()) {
                throw new AmqpRejectAndDontRequeueException("Invalid message " + messageId + ": " + fact);
            }
            notificationService.notify(messageId, fact);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY); // the listener thread takes the next message too
        }
    }
}
