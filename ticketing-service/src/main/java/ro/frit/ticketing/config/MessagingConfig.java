package ro.frit.ticketing.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

// Ticketing only publishes: it declares the exchange and says nothing about who listens.
// A topic exchange routes each message by its routing key to every queue bound with a matching pattern.
@Configuration
public class MessagingConfig {

    public static final String EXCHANGE = "ticketing.events";
    public static final String BOOKING_CONFIRMED = "booking.confirmed";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String BOOKED_EVENT_CHANGED = "booked.event-changed";

    @Bean
    TopicExchange ticketingExchange() {
        return new TopicExchange(EXCHANGE);
    }

    // Messages travel as JSON, with a unique message id each: the consumer uses it to ignore a repeated delivery.
    @Bean
    MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
        converter.setCreateMessageIds(true);
        return converter;
    }
}
