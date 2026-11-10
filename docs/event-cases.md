# Event cases — `/events`

Each row is one test in `EventControllerTest`. The controller tests replace the `TicketOffice` with a mock, so they check only the HTTP side: status, JSON, validation. The rules themselves are tested in `TicketOffice*Test`.

| # | Request | Situation | Status | The response has |
| --- | --- | --- | --- | --- |
| 1 | `GET /events` | | 200 | every event: `id`, `name`, `venueName`, `city`, `startsAt`, `capacity`, `availableSeats` |
| 2 | `GET /events?city=Iasi` | | 200 | only the upcoming events in that city |
| 3 | `GET /events/top` | | 200 | the 3 most booked events |
| 4 | `GET /events/top?limit=0` | not a positive number | 400 | a ProblemDetail |
| 5 | `GET /events/react-cluj` | the event exists | 200 | the event |
| 6 | `GET /events/nope` | no such event | 404 | `detail`: "No event with id nope" |
| 7 | `POST /events` | a valid body | 201 | the new event, and a `Location` header pointing to it |
| 8 | `POST /events` | blank name, start in the past, or capacity 0 | 400 | a ProblemDetail; the office is never called |

Every error is a ProblemDetail (`application/problem+json`): `type`, `title`, `status`, `detail`, `instance`.
