package ro.frit.ticketing.event;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import ro.frit.ticketing.event.dto.EventSearchRequest;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// Builds one MongoDB query from whichever filters were sent, with MongoTemplate and Criteria.
// The field names come from Lombok's @FieldNameConstants, so a renamed field breaks the build, not the query.
@RequiredArgsConstructor
public class EventSearchRepositoryImpl implements EventSearchRepository {

    private static final String CITY = Event.Fields.venue + "." + Venue.Fields.city;

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Event> search(EventSearchRequest filter) {
        // One Optional per filter: empty when the parameter was not sent (or blank), so flatMap drops it.
        // Pattern.quote makes the user's text plain text, not a regex: "C++" searches for "C++".
        List<Criteria> criteria = Stream.of(
                        text(filter.city()).map(city -> Criteria.where(CITY).regex("^" + Pattern.quote(city) + "$", "i")),
                        text(filter.name()).map(name -> Criteria.where(Event.Fields.name).regex(Pattern.quote(name), "i")),
                        Optional.ofNullable(filter.from()).map(from -> Criteria.where(Event.Fields.startsAt).gte(from)),
                        Optional.ofNullable(filter.to()).map(to -> Criteria.where(Event.Fields.startsAt).lt(to)))
                .flatMap(Optional::stream)
                .toList();

        // No filter matches every event; MongoDB rejects an $and with an empty list.
        Query query = criteria.isEmpty() ? new Query() : new Query(new Criteria().andOperator(criteria));
        return mongoTemplate.find(query.with(Sort.by(Event.Fields.startsAt)), Event.class);
    }

    private static Optional<String> text(String value) {
        return Optional.ofNullable(value).map(String::strip).filter(Predicate.not(String::isEmpty));
    }
}
