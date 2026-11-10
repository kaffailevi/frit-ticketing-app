package ro.frit.ticketing.event;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

// Spring Data writes the implementation. The query below comes from the method name:
// venue.city equal ignoring case, AND startsAt after the given moment, sorted by startsAt.
public interface EventRepository extends MongoRepository<Event, String> {

    List<Event> findByVenueCityIgnoreCaseAndStartsAtAfterOrderByStartsAt(String city, LocalDateTime after);
}
