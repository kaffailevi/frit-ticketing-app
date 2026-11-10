package ro.frit.ticketing.event;

import org.springframework.data.mongodb.repository.MongoRepository;

// Spring Data writes the CRUD methods; the search comes from the EventSearchRepository fragment.
public interface EventRepository extends MongoRepository<Event, String>, EventSearchRepository {
}
