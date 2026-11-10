package ro.frit.notification.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// The consumer owns its queues. For each fact ticketing publishes:
//   a durable queue bound to the exchange with the fact's routing key, so messages wait while this service is down;
//   a dead-letter queue: a message this service refuses (invalid) is moved there instead of being lost or retried forever.
@Configuration
public class MessagingConfig {

    public static final String EXCHANGE = "ticketing.events";
    public static final String DEAD_LETTER_EXCHANGE = "notification.dlx";

    public static final String BOOKING_CONFIRMED_QUEUE = "notification.booking-confirmed";
    public static final String BOOKING_CANCELLED_QUEUE = "notification.booking-cancelled";
    public static final String BOOKED_EVENT_CHANGED_QUEUE = "notification.booked-event-changed";

    // queue -> the routing key it listens to (the same keys ticketing sends with)
    private static final Map<String, String> QUEUES = Map.of(
            BOOKING_CONFIRMED_QUEUE, "booking.confirmed",
            BOOKING_CANCELLED_QUEUE, "booking.cancelled",
            BOOKED_EVENT_CHANGED_QUEUE, "booked.event-changed");

    public static String deadLetterQueueOf(String queue) {
        return queue + ".dlq";
    }

    // Everything RabbitMQ must have, declared on startup (declaring what exists already changes nothing).
    // The exchange is declared here too: either service may start first, and a binding needs its exchange.
    @Bean
    Declarables bookingFactQueues() {
        TopicExchange exchange = new TopicExchange(EXCHANGE);
        DirectExchange deadLetters = new DirectExchange(DEAD_LETTER_EXCHANGE);
        List<Declarable> declarables = new ArrayList<>(List.of(exchange, deadLetters));
        QUEUES.forEach((queueName, routingKey) -> {
            Queue queue = QueueBuilder.durable(queueName)
                    .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                    .deadLetterRoutingKey(deadLetterQueueOf(queueName))
                    .build();
            Queue deadLetterQueue = QueueBuilder.durable(deadLetterQueueOf(queueName)).build();
            declarables.add(queue);
            declarables.add(BindingBuilder.bind(queue).to(exchange).with(routingKey));
            declarables.add(deadLetterQueue);
            declarables.add(BindingBuilder.bind(deadLetterQueue).to(deadLetters).with(deadLetterQueueOf(queueName)));
        });
        return new Declarables(declarables);
    }

    // JSON in; the target type comes from the listener method's parameter, not from ticketing's class names.
    @Bean
    MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
