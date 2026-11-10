# Booking cases

What must happen when someone books seats. Each row is one test, named after the expected behaviour. The tests were written from this table before the code (red, then green).

## The rules — `EventServiceTest`, `BookingServiceTest` (against a real MongoDB)

The event used in the examples: **e1**, capacity 10, 8 seats already booked (2 free), starting tomorrow at 19:00. "Now" is fixed in the tests.

| # | Situation | Request | Result |
| --- | --- | --- | --- |
| 1 | The event does not exist | `unknown`, 1 seat | `EventNotFoundException`; nothing is stored |
| 2 | Enough free seats | `e1`, 1 seat | booked; 1 seat left |
| 3 | Exactly the free seats | `e1`, 2 seats | booked; 0 seats left |
| 4 | More seats than are free | `e1`, 3 seats | `NotEnoughSeatsException` with the 2 free seats; nothing changes, no booking is stored |
| 5 | The event has already started | an event that started 1 minute ago | `EventAlreadyStartedException`; nothing changes |
| 6 | The event starts right now | an event whose start time is now | `EventAlreadyStartedException` |
| 7 | A confirmed booking | `e1`, 2 seats | stored with who booked and when; readable only under `e1` |

Invalid requests (0 seats, no email) never reach the services: Bean Validation refuses them in the controller (HTTP rows 6–8).

> Session 1 kept these rules in an in-memory `TicketOffice` that returned a sealed `BookingResult`. Since session 3 the services store everything in MongoDB and throw exceptions, which `GlobalExceptionHandler` turns into HTTP answers.

## Over HTTP — `POST /events/{eventId}/bookings` — `BookingControllerTest`

Body: `{"customerEmail": "ana@example.com", "seats": 2}`. The service is a mock there, so these rows check only how each outcome travels over HTTP.

| # | Situation | Status | The response has |
| --- | --- | --- | --- |
| 1 | The service confirms | 201 | the booking (`id`, `eventId`, `customerEmail`, `seats`, `bookedAt`) and a `Location` header to read it |
| 2 | The body also has an `eventId` | 201 | the path decides; the body's `eventId` is ignored |
| 3 | No such event | 404 | `detail`: "No event with id e9" |
| 4 | Not enough seats | 409 | `availableSeats` |
| 5 | The event has already started | 409 | `startsAt` |
| 6 | Email blank or not an email | 400 | the service is never called |
| 7 | Seats 0, negative or missing | 400 | the service is never called |
| 8 | Broken JSON, or seats is not a number | 400 | the service is never called |
| 9 | Not JSON | 415 | the service is never called |
| 10 | `GET /events/e1/bookings/b1` | 200 | the booking |
| 11 | `GET` a booking through another event's URL | 404 | a booking is only visible under its own event |

Every error is a ProblemDetail (`application/problem+json`).

## Not decided yet

- Can the same customer book the same event twice? Today, yes.
- Two people booking the last seats at the same moment: both could read "2 free". Fixed in session 4 (optimistic locking).
