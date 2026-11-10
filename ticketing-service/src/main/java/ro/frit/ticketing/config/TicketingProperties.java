package ro.frit.ticketing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDateTime;
import java.util.List;

// Everything under "ticketing:" in application.yml, bound to a record. Kebab-case keys map to camelCase fields.
@ConfigurationProperties(prefix = "ticketing")
public record TicketingProperties(List<SeedEvent> seedEvents) {

    public TicketingProperties {
        seedEvents = seedEvents == null ? List.of() : List.copyOf(seedEvents);
    }

    public record SeedEvent(String id, String name, String venueName, String city, LocalDateTime startsAt, int capacity) {
    }
}
