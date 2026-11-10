# Booking cases

What must happen when someone books seats. Each row is one test in `TicketOfficeTest`, named after the expected behaviour. The tests were written from this table before the code (red, then green).

The event used in the examples: **e1**, capacity 10, 8 seats already booked (2 free), starting tomorrow at 19:00.

| # | Situation | Request | Result |
| --- | --- | --- | --- |
| 1 | The event does not exist | `unknown`, 1 seat | **UnknownEvent** `unknown` |
| 2 | Enough free seats | `e1`, 1 seat | **Confirmed**; 1 seat left |
| 3 | Exactly the free seats | `e1`, 2 seats | **Confirmed**; 0 seats left |
| 4 | More seats than are free | `e1`, 3 seats | **SoldOut**, reporting the 2 free seats; nothing changes |
| 5 | The event has already started | an event that started 1 minute ago | **AlreadyStarted**; nothing changes |
| 6 | The event starts right now | an event whose start time is now | **AlreadyStarted** |
| 7 | Two events with the same id | creating the TicketOffice | refused (IllegalArgumentException) |

Invalid requests (0 seats, no email, no event id) never reach the TicketOffice: `BookingRequest` refuses them when it is created (`BookingRequestTest`).
