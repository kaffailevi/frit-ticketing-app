package ro.frit.ticketing.event;

import ro.frit.ticketing.event.dto.EventSearchRequest;

import java.util.List;

// A repository fragment: a query that a method name cannot express (any combination of optional filters).
// EventRepository extends this interface; Spring Data finds the implementation by its name, ...Impl.
public interface EventSearchRepository {

    List<Event> search(EventSearchRequest filter);
}
