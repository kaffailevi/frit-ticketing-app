package ro.frit.ticketing.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;
import ro.frit.ticketing.event.dto.EventSearchRequest;

import java.net.URI;
import java.util.List;

// Cases: docs/event-cases.md. Only the happy path lives here; errors are exceptions that
// GlobalExceptionHandler turns into ProblemDetail responses.
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // GET /events?city=Iasi&name=cloud&from=2027-03-01T00:00:00&to=2027-04-01T00:00:00 — every filter optional.
    @GetMapping
    public List<EventResponse> search(EventSearchRequest filter) {
        return eventService.search(filter);
    }

    // "/top" is more specific than "/{id}", so Spring picks this method for GET /events/top.
    @GetMapping("/top")
    public List<EventResponse> topEvents(@RequestParam(defaultValue = "3") @Positive @Max(50) int limit) {
        return eventService.findMostBooked(limit);
    }

    @GetMapping("/{id}")
    public EventResponse event(@PathVariable String id) {
        return eventService.findById(id);
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest body) {
        EventResponse event = eventService.create(body);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(event.id()).toUri();
        return ResponseEntity.created(location).body(event);
    }
}
