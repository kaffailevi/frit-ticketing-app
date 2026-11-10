package ro.frit.ticketing.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import ro.frit.ticketing.event.Event;

// Events stored before session 4 have no "version" field. Spring Data would take them for new events and try
// to insert them a second time (a duplicate key, HTTP 500), so at startup they get version 0, in one update.
// Runs before DataSeeder. Does nothing once every event has a version.
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class EventVersionMigration implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        long updated = mongoTemplate.updateMulti(
                Query.query(Criteria.where(Event.Fields.version).exists(false)),
                Update.update(Event.Fields.version, 0L),
                Event.class).getModifiedCount();
        if (updated > 0) {
            log.info("Gave a version to {} events stored before optimistic locking", updated);
        }
    }
}
