package ro.frit.ticketing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ro.frit.ticketing.domain.Event;
import ro.frit.ticketing.domain.TicketOffice;
import ro.frit.ticketing.domain.Venue;

import java.time.Clock;

// The domain classes know nothing about Spring; this class is where they become beans.
@Configuration
public class TicketingConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public TicketOffice ticketOffice(TicketingProperties properties, Clock clock) {
        return new TicketOffice(properties.seedEvents().stream().map(TicketingConfig::toEvent).toList(), clock);
    }

    private static Event toEvent(TicketingProperties.SeedEvent seed) {
        return Event.builder()
                .id(seed.id())
                .name(seed.name())
                .venue(new Venue(seed.venueName(), seed.city()))
                .startsAt(seed.startsAt())
                .capacity(seed.capacity())
                .bookedSeats(0)
                .build();
    }
}
