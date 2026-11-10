package ro.frit.ticketing.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ro.frit.ticketing.config.FixedClockConfiguration.TODAY_AT_NOON;
import static ro.frit.ticketing.event.EventFixtures.event;
import static ro.frit.ticketing.event.EventFixtures.tomorrowEvening;

// Reserving seats: the event rows of docs/booking-cases.md. Listing: the queries behind docs/event-cases.md.
@MongoPersistenceTest
@Import(EventService.class)
class EventServiceTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventService eventService;

    @BeforeEach
    void emptyCollection() {
        eventRepository.deleteAll();
    }

    private int bookedSeats(String eventId) {
        return eventRepository.findById(eventId).orElseThrow().getBookedSeats();
    }

    @Test
    void reservingFreeSeatsAddsThemToTheEvent() {
        eventRepository.save(tomorrowEvening("e1"));

        eventService.reserveSeats("e1", 1);

        assertEquals(9, bookedSeats("e1"));
    }

    @Test
    void theLastFreeSeatsCanBeReserved() {
        eventRepository.save(tomorrowEvening("e1"));

        eventService.reserveSeats("e1", 2);

        assertEquals(10, bookedSeats("e1"));
    }

    @Test
    void reservingMoreSeatsThanAreFreeIsRefusedAndChangesNothing() {
        eventRepository.save(tomorrowEvening("e1"));

        NotEnoughSeatsException refused = assertThrows(NotEnoughSeatsException.class, () -> eventService.reserveSeats("e1", 3));

        assertEquals(2, refused.properties().get("availableSeats"));
        assertEquals(8, bookedSeats("e1"));
    }

    @Test
    void anUnknownEventIsReported() {
        assertThrows(EventNotFoundException.class, () -> eventService.reserveSeats("unknown", 1));
    }

    @Test
    void anEventThatHasStartedCannotBeBooked() {
        eventRepository.save(event("e2", "Iasi", TODAY_AT_NOON.minusMinutes(1), 10, 0));

        assertThrows(EventAlreadyStartedException.class, () -> eventService.reserveSeats("e2", 1));
        assertEquals(0, bookedSeats("e2"));
    }

    @Test
    void anEventStartingRightNowCannotBeBooked() {
        eventRepository.save(event("e3", "Iasi", TODAY_AT_NOON, 10, 0));

        assertThrows(EventAlreadyStartedException.class, () -> eventService.reserveSeats("e3", 1));
    }

    @Test
    void aCreatedEventGetsAnIdAndAllItsSeatsFree() {
        EventResponse created = eventService.create(new CreateEventRequest(
                "Kubernetes Basics", "Cowork", "Timisoara", TODAY_AT_NOON.plusDays(20), 25));

        assertEquals(created, eventService.findById(created.id()));
        assertEquals(25, created.availableSeats());
    }

    @Test
    void theMostBookedEventsComeFirstAndTiesAreOrderedByName() {
        eventRepository.saveAll(List.of(
                event("a", "Iasi", TODAY_AT_NOON.plusDays(1), 50, 5),
                event("b", "Iasi", TODAY_AT_NOON.plusDays(1), 50, 40),
                event("c", "Iasi", TODAY_AT_NOON.plusDays(1), 50, 5),
                event("d", "Iasi", TODAY_AT_NOON.plusDays(1), 50, 1)));

        List<String> top = eventService.findMostBooked(3).stream().map(EventResponse::id).toList();

        assertEquals(List.of("b", "a", "c"), top);
    }

    @Test
    void upcomingEventsInACityDoNotIncludeEventsThatAlreadyStarted() {
        eventRepository.saveAll(List.of(
                event("past", "Iasi", TODAY_AT_NOON.minusHours(1), 10, 0),
                event("next", "Iasi", TODAY_AT_NOON.plusHours(1), 10, 0)));

        List<String> upcoming = eventService.findUpcomingIn("iasi").stream().map(EventResponse::id).toList();

        assertEquals(List.of("next"), upcoming);
    }
}
