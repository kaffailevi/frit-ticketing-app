package ro.frit.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ro.frit.notification.config.TestcontainersConfiguration;
import ro.frit.notification.messages.BookedEventChanged;
import ro.frit.notification.messages.BookingCancelled;
import ro.frit.notification.messages.BookingConfirmed;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ro.frit.notification.config.MessagingConfig.BOOKING_CONFIRMED_QUEUE;
import static ro.frit.notification.config.MessagingConfig.EXCHANGE;
import static ro.frit.notification.config.MessagingConfig.deadLetterQueueOf;

// The whole consumer side against a real RabbitMQ and MongoDB: a message is sent to the exchange exactly as
// ticketing sends it (routing key + message id), and the test waits until the listener has handled it.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingEventsListenerTest {

    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2027, 2, 10, 18, 0);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void noNotifications() {
        notificationRepository.deleteAll();
    }

    private void send(String routingKey, Object fact, String messageId) {
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey, fact, message -> {
            message.getMessageProperties().setMessageId(messageId);
            return message;
        });
    }

    private List<Notification> waitForNotifications(int count) {
        await().atMost(Duration.ofSeconds(10)).until(() -> notificationRepository.count() == count);
        return notificationRepository.findAll();
    }

    private static BookingConfirmed confirmed(int seats) {
        return new BookingConfirmed("b1", "react-cluj", "React Workshop", STARTS_AT, "ana@example.com", seats,
                LocalDateTime.of(2026, 11, 30, 12, 0));
    }

    @Test
    void aConfirmedBookingBecomesANotificationForTheCustomer() {
        send("booking.confirmed", confirmed(2), "m-1");

        Notification notification = waitForNotifications(1).getFirst();

        assertEquals("ana@example.com", notification.getCustomerEmail());
        assertEquals("2 seat(s) for React Workshop on 10 Feb 2027 at 18:00 are confirmed.", notification.getText());
    }

    @Test
    void aBookingCancelledBecauseTheEventWasCalledOffSaysSo() {
        send("booking.cancelled", new BookingCancelled("b1", "react-cluj", "ana@example.com", 2,
                BookingCancelled.Reason.EVENT_CANCELLED), "m-2");

        assertEquals("2 seat(s) for event react-cluj are cancelled: the event was called off.",
                waitForNotifications(1).getFirst().getText());
    }

    @Test
    void aChangedEventTellsTheNewPlaceAndTime() {
        send("booked.event-changed", new BookedEventChanged("b1", "react-cluj", "ana@example.com", 2,
                "React Workshop", "New Hall", "Iasi", STARTS_AT.plusDays(1)), "m-3");

        assertEquals("Your event changed: 2 seat(s) for React Workshop, now at New Hall, Iasi, on 11 Feb 2027 at 18:00.",
                waitForNotifications(1).getFirst().getText());
    }

    @Test
    void theSameMessageDeliveredTwiceIsStoredOnce() throws InterruptedException {
        send("booking.confirmed", confirmed(2), "m-same");
        send("booking.confirmed", confirmed(2), "m-same");

        waitForNotifications(1);
        Thread.sleep(500); // give the second copy time to arrive, then check it was not stored
        assertEquals(1, notificationRepository.count());
    }

    @Test
    void anInvalidMessageGoesToTheDeadLetterQueueAndIsNotStored() {
        send("booking.confirmed", confirmed(0), "m-bad"); // 0 seats: invalid

        Message deadLetter = await().atMost(Duration.ofSeconds(10))
                .until(() -> rabbitTemplate.receive(deadLetterQueueOf(BOOKING_CONFIRMED_QUEUE)), message -> message != null);

        assertNotNull(deadLetter);
        assertEquals("m-bad", deadLetter.getMessageProperties().getMessageId());
        assertEquals(0, notificationRepository.count());
    }
}
