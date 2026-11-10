package ro.frit.ticketing.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.config.RabbitTestcontainersConfiguration;
import ro.frit.ticketing.config.TestcontainersConfiguration;
import ro.frit.ticketing.shared.web.CorrelationIdFilter;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ro.frit.ticketing.config.MessagingConfig.BOOKING_CANCELLED;
import static ro.frit.ticketing.config.MessagingConfig.BOOKING_CONFIRMED;
import static ro.frit.ticketing.config.MessagingConfig.EXCHANGE;

// The RabbitMQ adapter against a real broker: a test queue listens on the exchange like a consumer would,
// and receives what the adapter sends, as JSON, with a message id and the right routing key.
@SpringBootTest(properties = "spring.mongodb.database=messaging-test")
@Import({TestcontainersConfiguration.class, RabbitTestcontainersConfiguration.class})
class RabbitBookingEventPublisherTest {

    private static final String TEST_QUEUE = "test.booking-facts";

    @Autowired
    private RabbitBookingEventPublisher publisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @BeforeEach
    void aQueueBoundToEveryBookingFact() {
        Queue queue = new Queue(TEST_QUEUE, true); // durable: RabbitMQ 4 refuses transient shared queues
        amqpAdmin.declareQueue(queue);
        amqpAdmin.purgeQueue(TEST_QUEUE, false);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(new TopicExchange(EXCHANGE)).with("booking.*"));
    }

    private Message receive() {
        Message message = rabbitTemplate.receive(TEST_QUEUE, 5_000);
        assertNotNull(message, "a message arrived");
        return message;
    }

    @Test
    void aConfirmedBookingIsSentAsJsonWithItsRoutingKeyAndAMessageId() {
        publisher.publish(new BookingConfirmed("b1", "react-cluj", "React Workshop", LocalDateTime.of(2027, 2, 10, 18, 0),
                "ana@example.com", 2, LocalDateTime.of(2026, 11, 30, 12, 0)));

        Message message = receive();
        String json = new String(message.getBody(), StandardCharsets.UTF_8);

        assertEquals(BOOKING_CONFIRMED, message.getMessageProperties().getReceivedRoutingKey());
        assertNotNull(message.getMessageProperties().getMessageId(), "every message has an id");
        assertTrue(json.contains("\"customerEmail\":\"ana@example.com\""), json);
        assertTrue(json.contains("\"eventName\":\"React Workshop\""), json);
    }

    @Test
    void theRequestsCorrelationIdTravelsWithTheMessage() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "corr-42"); // what CorrelationIdFilter does for an HTTP request
        try {
            publisher.publish(new BookingCancelled("b1", "react-cluj", "ana@example.com", 1, BookingCancelled.Reason.BY_CUSTOMER));
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }

        assertEquals("corr-42", receive().getMessageProperties().getHeader(CorrelationIdFilter.HEADER));
    }

    @Test
    void aCancellationTravelsWithItsReason() {
        publisher.publish(new BookingCancelled("b1", "react-cluj", "ana@example.com", 2, BookingCancelled.Reason.EVENT_CANCELLED));

        Message message = receive();

        assertEquals(BOOKING_CANCELLED, message.getMessageProperties().getReceivedRoutingKey());
        assertTrue(new String(message.getBody(), StandardCharsets.UTF_8).contains("\"reason\":\"EVENT_CANCELLED\""));
    }
}
