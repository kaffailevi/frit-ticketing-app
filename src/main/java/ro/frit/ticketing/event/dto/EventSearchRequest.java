package ro.frit.ticketing.event.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

// The query parameters of GET /events, bound by name to this record. A parameter that is not sent stays null.
//   city: the whole city name, any case     name: part of the event's name, any case
//   from / to: start time range, ISO format (2027-02-01T00:00:00); without "from" only upcoming events are listed
public record EventSearchRequest(String city,
                                 String name,
                                 @DateTimeFormat(iso = DATE_TIME) LocalDateTime from,
                                 @DateTimeFormat(iso = DATE_TIME) LocalDateTime to) {

    public static EventSearchRequest none() {
        return new EventSearchRequest(null, null, null, null);
    }

    public EventSearchRequest withFrom(LocalDateTime newFrom) {
        return new EventSearchRequest(city, name, newFrom, to);
    }
}
