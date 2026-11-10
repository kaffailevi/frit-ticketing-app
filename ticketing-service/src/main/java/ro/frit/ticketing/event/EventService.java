package ro.frit.ticketing.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventSearchRequest;
import ro.frit.ticketing.event.dto.UpdateEventRequest;
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
    private static final Sort MOST_BOOKED_FIRST = Sort.by(Sort.Order.desc(Event.Fields.bookedSeats), Sort.Order.asc(Event.Fields.name));

    private final EventRepository eventRepository;
    private final Clock clock;
    private final ApplicationEventPublisher applicationEvents; // in-process events: EventChanged, EventCancelled

    // Without a "from", only events that have not started yet are listed.
    public List<EventResponse> search(EventSearchRequest filter) {
        EventSearchRequest effective = filter.from() == null ? filter.withFrom(now()) : filter;
        return toResponses(eventRepository.search(effective));
    }

    public boolean exists(String id) {
        return eventRepository.existsById(id);
    }

    public EventResponse findById(String id) {
        return EventResponse.from(getEvent(id));
    }

    // The sorting and the limit run inside MongoDB; only the requested events travel to the application.
    public List<EventResponse> findMostBooked(int limit) {
        return toResponses(eventRepository.findAll(PageRequest.of(0, limit, MOST_BOOKED_FIRST)).getContent());
    }

    public EventResponse create(CreateEventRequest request) {
        return EventResponse.from(eventRepository.insert(request.toEvent(UUID.randomUUID().toString())));
    }

    // PUT replaces the details; the booked seats stay. The capacity can never go below the seats already sold.
    public EventResponse update(String id, UpdateEventRequest request) {
        Event event = getEvent(id);
        if (request.capacity() < event.getBookedSeats()) {
            throw new CapacityBelowSoldSeatsException(id, event.getBookedSeats());
        }
        Event updated = eventRepository.save(event.withDetails(request.name(),
                new Venue(request.venueName(), request.city()), request.startsAt(), request.capacity()));
        if (updated.differsForCustomersFrom(event)) {
            applicationEvents.publishEvent(EventChanged.of(updated));
        }
        return EventResponse.from(updated);
    }

    // The event is deleted, and everyone who listens is told; BookingService cancels its bookings.
    public void cancel(String id) {
        Event event = getEvent(id);
        eventRepository.delete(event);
        applicationEvents.publishEvent(EventCancelled.of(event));
    }

    // Called by BookingService before it records a booking. Throws when the seats cannot be reserved.
    // When two bookings for the same event save at the same moment, the second save fails (optimistic locking);
    // Spring then calls this method again, which reads the event afresh: the booking either fits or is sold out.
    // After 3 retries the OptimisticLockingFailureException reaches GlobalExceptionHandler as a 409.
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 20, jitter = 10)
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

    // Called by BookingService when a booking is cancelled: the seats go back on sale, unless the event has started.
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3, delay = 20, jitter = 10)
    public void releaseSeats(String eventId, int seats) {
        Event event = getEvent(eventId);
        if (event.hasStartedAt(now())) {
            throw new EventAlreadyStartedException(eventId, event.getStartsAt());
        }
        eventRepository.save(event.withSeatsReleased(seats));
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
