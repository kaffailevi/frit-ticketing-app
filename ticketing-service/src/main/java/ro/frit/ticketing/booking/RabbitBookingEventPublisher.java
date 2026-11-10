package ro.frit.ticketing.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static ro.frit.ticketing.config.MessagingConfig.BOOKED_EVENT_CHANGED;
import static ro.frit.ticketing.config.MessagingConfig.BOOKING_CANCELLED;
import static ro.frit.ticketing.config.MessagingConfig.BOOKING_CONFIRMED;
import static ro.frit.ticketing.config.MessagingConfig.EXCHANGE;

// The second adapter of the BookingEventPublisher port (session 5 had one that only logged). BookingService did not
// change: it still calls publish(...), and now each fact leaves the service as a JSON message on RabbitMQ.
@Component
@RequiredArgsConstructor
public class RabbitBookingEventPublisher implements BookingEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(BookingConfirmed confirmed) {
        rabbitTemplate.convertAndSend(EXCHANGE, BOOKING_CONFIRMED, confirmed);
    }

    @Override
    public void publish(BookingCancelled cancelled) {
        rabbitTemplate.convertAndSend(EXCHANGE, BOOKING_CANCELLED, cancelled);
    }

    @Override
    public void publish(BookedEventChanged changed) {
        rabbitTemplate.convertAndSend(EXCHANGE, BOOKED_EVENT_CHANGED, changed);
    }
}
