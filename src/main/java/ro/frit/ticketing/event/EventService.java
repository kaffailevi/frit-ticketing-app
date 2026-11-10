package ro.frit.ticketing.event;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// The rules about events: what can be listed, and whether seats can be reserved (docs/booking-cases.md).
@Service
@RequiredArgsConstructor
public class EventService {

    // Most booked first; on a tie, by name, so the order is always the same.
    private static final Sort MOST_BOOKED_FIRST = Sort.by(Sort.Order.desc("bookedSeats"), Sort.Order.asc("name"));

    private final EventRepository eventRepository;
    private final Clock clock;

    public List<EventResponse> findAll() {
        return toResponses(eventRepository.findAll());
    }

    public EventResponse findById(String id) {
        return EventResponse.from(getEvent(id));
    }

    public List<EventResponse> findUpcomingIn(String city) {
        return toResponses(eventRepository.findByVenueCityIgnoreCaseAndStartsAtAfterOrderByStartsAt(city, now()));
    }

    // The sorting and the limit run inside MongoDB; only the requested events travel to the application.
    public List<EventResponse> findMostBooked(int limit) {
        return toResponses(eventRepository.findAll(PageRequest.of(0, limit, MOST_BOOKED_FIRST)).getContent());
    }

    public EventResponse create(CreateEventRequest request) {
        return EventResponse.from(eventRepository.insert(request.toEvent(UUID.randomUUID().toString())));
    }

    // Called by BookingService before it records a booking. Throws when the seats cannot be reserved.
    public void reserveSeats(String eventId, int seats) {
        Event event = getEvent(eventId);
        if (event.hasStartedAt(now())) {
            throw new EventAlreadyStartedException(eventId, event.getStartsAt());
        }
        if (seats > event.availableSeats()) {
            throw new NotEnoughSeatsException(eventId, event.availableSeats());
        }
        eventRepository.save(event.withSeatsBooked(seats));
    }

    private Event getEvent(String id) {
        return eventRepository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static List<EventResponse> toResponses(List<Event> events) {
        return events.stream().map(EventResponse::from).toList();
    }
}
