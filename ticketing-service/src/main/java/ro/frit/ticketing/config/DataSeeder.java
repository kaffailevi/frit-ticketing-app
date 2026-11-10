package ro.frit.ticketing.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ro.frit.ticketing.event.Event;
import ro.frit.ticketing.event.EventRepository;
import ro.frit.ticketing.event.Venue;

// Runs once after the application has started. On an empty database (a fresh Docker volume, a new Atlas cluster)
// it stores the learning events from application.yml; when events exist, it leaves them alone,
// so bookings made earlier are never reset by a restart.
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final TicketingProperties properties;
    private final EventRepository eventRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (eventRepository.count() > 0) {
            log.info("Events already stored, nothing to seed");
            return;
        }
        eventRepository.insert(properties.seedEvents().stream().map(DataSeeder::toEvent).toList());
        log.info("Seeded {} events", properties.seedEvents().size());
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
