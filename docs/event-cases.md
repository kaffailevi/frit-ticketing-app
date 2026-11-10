# Event cases — `/events`

Each row is one test in `EventControllerTest`. The controller tests replace `EventService` with a mock, so they check only the HTTP side: status, JSON, validation. The rules themselves are tested against a real MongoDB in `EventServiceTest` and `EventSearchRepositoryTest`.

| # | Request | Situation | Status | The response has |
| --- | --- | --- | --- | --- |
| 1 | `GET /events` | no filter | 200 | the events that have not started yet, soonest first: `id`, `name`, `venueName`, `city`, `startsAt`, `capacity`, `availableSeats` |
| 2 | `GET /events?city=Iasi&name=cloud&from=…&to=…` | any combination of filters | 200 | the matching events (search rules: `EventSearchRepositoryTest`) |
| 2b | `GET /events?from=next week` | a date that is not ISO | 400 | a ProblemDetail |
| 3 | `GET /events/top` | | 200 | the 3 most booked events |
| 4 | `GET /events/top?limit=0` | not a positive number | 400 | a ProblemDetail |
| 5 | `GET /events/react-cluj` | the event exists | 200 | the event |
| 6 | `GET /events/nope` | no such event | 404 | `detail`: "No event with id nope" |
| 7 | `POST /events` | a valid body | 201 | the new event, and a `Location` header pointing to it |
| 8 | `POST /events` | blank name, start in the past, or capacity 0 | 400 | a ProblemDetail; the service is never called |
| 9 | `PUT /events/cloud-iasi` | a valid body | 200 | the updated event; the booked seats stay |
| 10 | `PUT /events/cloud-iasi` | capacity below the seats already sold | 409 | `bookedSeats` |
| 11 | `PUT /events/cloud-iasi` | an invalid body | 400 | the service is never called |
| 12 | `DELETE /events/cloud-iasi` | the event exists | 204 | no body (session 5: its bookings are cancelled too) |
| 13 | `DELETE /events/nope` | no such event | 404 | a ProblemDetail |

Every error is a ProblemDetail (`application/problem+json`): `type`, `title`, `status`, `detail`, `instance`.
