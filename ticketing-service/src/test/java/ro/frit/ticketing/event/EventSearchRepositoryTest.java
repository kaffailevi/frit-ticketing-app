package ro.frit.ticketing.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.event.dto.EventSearchRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ro.frit.ticketing.config.FixedClockConfiguration.TODAY_AT_NOON;

// The search rules of GET /events, against a real MongoDB. Every filter is optional; together they all apply.
@MongoPersistenceTest
class EventSearchRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void storeEvents() {
        eventRepository.deleteAll();
        eventRepository.saveAll(List.of(
                event("react", "React Workshop", "Cluj-Napoca", 10),
                event("spring", "Spring Boot Meetup", "cluj-napoca", 5),
                event("cloud", "Cloud Run Hands-on", "Iasi", 20),
                event("cpp", "Modern C++ Evening", "Cluj-Napoca Est", 15),
                event("past", "Past React Talk", "Cluj-Napoca", -3)));
    }

    private static Event event(String id, String name, String city, int daysFromToday) {
        return Event.builder()
                .id(id).name(name).venue(new Venue("Venue " + id, city))
                .startsAt(TODAY_AT_NOON.plusDays(daysFromToday)).capacity(10).bookedSeats(0)
                .build();
    }

    private List<String> ids(EventSearchRequest filter) {
        return eventRepository.search(filter).stream().map(Event::getId).toList();
    }

    @Test
    void withoutFiltersEveryEventIsFoundSoonestFirst() {
        assertEquals(List.of("past", "spring", "react", "cpp", "cloud"), ids(EventSearchRequest.none()));
    }

    @Test
    void theCityMustMatchWhollyButInAnyCase() {
        assertEquals(List.of("past", "spring", "react"), ids(new EventSearchRequest("CLUJ-napoca", null, null, null)));
    }

    @Test
    void theNameMatchesAnyPartInAnyCase() {
        assertEquals(List.of("past", "react"), ids(new EventSearchRequest(null, "react", null, null)));
    }

    @Test
    void searchTextIsPlainTextNotARegularExpression() {
        assertEquals(List.of("cpp"), ids(new EventSearchRequest(null, "c++", null, null)));
    }

    @Test
    void theDateRangeIncludesFromAndExcludesTo() {
        EventSearchRequest range = new EventSearchRequest(null, null, TODAY_AT_NOON.plusDays(5), TODAY_AT_NOON.plusDays(15));

        assertEquals(List.of("spring", "react"), ids(range));
    }

    @Test
    void filtersAreCombined() {
        EventSearchRequest filter = new EventSearchRequest("Cluj-Napoca", "react", TODAY_AT_NOON, null);

        assertEquals(List.of("react"), ids(filter));
    }

    @Test
    void blankTextFiltersAreIgnored() {
        assertEquals(5, ids(new EventSearchRequest("  ", "", null, null)).size());
    }
}
