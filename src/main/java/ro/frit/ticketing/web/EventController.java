package ro.frit.ticketing.web;

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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ro.frit.ticketing.domain.Event;
import ro.frit.ticketing.domain.TicketOffice;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

// Cases: docs/event-cases.md. The controller translates HTTP to TicketOffice calls and back; no rules live here.
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final TicketOffice ticketOffice;

    @GetMapping
    public List<EventResponse> events(@RequestParam(required = false) String city) {
        List<Event> events = city == null || city.isBlank() ? ticketOffice.allEvents() : ticketOffice.upcomingEventsIn(city);
        return events.stream().map(EventResponse::from).toList();
    }

    // "/top" is more specific than "/{id}", so Spring picks this method for GET /events/top.
    @GetMapping("/top")
    public List<EventResponse> topEvents(@RequestParam(defaultValue = "3") @Positive @Max(50) int limit) {
        return ticketOffice.topEventsByBookedSeats(limit).stream().map(EventResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EventResponse event(@PathVariable String id) {
        return ticketOffice.findEvent(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No event with id " + id));
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest body) {
        Event event = ticketOffice.addEvent(body.toEvent(UUID.randomUUID().toString()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(event.getId()).toUri();
        return ResponseEntity.created(location).body(EventResponse.from(event));
    }
}
