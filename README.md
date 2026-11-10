# Ticketing app

[![Build](https://github.com/kaffailevi/frit-ticketing-app/actions/workflows/build.yml/badge.svg)](https://github.com/kaffailevi/frit-ticketing-app/actions/workflows/build.yml)

My version of the course project from **FutureReady IT — Module 2: modern architecture with Java and Cloud**: a small distributed ticketing system for learning events, built session by session and deployed to Google Cloud.

## Stack

Java 25 · Spring Boot 4.1 (Web MVC, Bean Validation, ProblemDetail) · MongoDB 8 with Spring Data (Docker locally, Atlas in the cloud) · RabbitMQ 4 (Docker locally, CloudAMQP in the cloud) · Testcontainers · Spring Cloud Gateway (WebMVC) + Bucket4j · ArchUnit · Gradle 9 multi-module (Kotlin DSL, version catalog) · JUnit 6 + MockMvc · GitHub Actions

## Services

| Module | Port | What it does |
| --- | --- | --- |
| [ticketing-service](ticketing-service) | 8085 | events and bookings (MongoDB `ticketing`); publishes booking facts to RabbitMQ |
| [notification-service](notification-service) | 8086 | consumes those facts and stores one notification per message (MongoDB `notifications`) |
| [api-gateway](api-gateway) | 8080 | the one door for clients: routes `/events/**` and `/notifications/**`, rate limit (429), `X-Correlation-Id`, API key |

## Run it

Locally, with MongoDB and RabbitMQ in Docker (each service in its own terminal):

```bash
docker compose up -d                          # MongoDB :27017, RabbitMQ :5672 (UI :15672, guest/guest)
./gradlew :ticketing-service:bootRun          # http://localhost:8085/events — the learning events are seeded on the first start
./gradlew :notification-service:bootRun       # http://localhost:8086/notifications
./gradlew :api-gateway:bootRun                # http://localhost:8080/events — the way clients come in
```

If another program already uses port 8080, start the gateway with `SERVER_PORT=8090`. Set `GATEWAY_API_KEY` to require an `X-Api-Key` header (always in the cloud; see [http/gateway.http](http/gateway.http)).

### Following one request

Every request gets an `X-Correlation-Id` at the gateway (or keeps the one the client sent). Every log line shows it, `INFO [demo-trace-1] …`, in the gateway, in ticketing, and, carried in the RabbitMQ message header, in the notification service.

Against MongoDB Atlas and CloudAMQP (`cloud` profile). It reads `MONGODB_HOST` (e.g. `cluster0.abcde.mongodb.net`), `MONGODB_USERNAME`, `MONGODB_PASSWORD`, `CLOUDAMQP_HOST`, `CLOUDAMQP_USERNAME` and `CLOUDAMQP_PASSWORD` from the environment, never from a file:

```bash
./gradlew :ticketing-service:bootRun --args='--spring.profiles.active=cloud'
./gradlew :notification-service:bootRun --args='--spring.profiles.active=cloud'
```

Try it with the request files in [http/](http/) (IntelliJ HTTP client or VS Code REST Client). What each endpoint answers, including the errors: [docs/event-cases.md](docs/event-cases.md) and [docs/booking-cases.md](docs/booking-cases.md).

| Method | Path | |
| --- | --- | --- |
| GET | `/events?city=&name=&from=&to=` | search; without `from` only upcoming events |
| GET | `/events/top?limit=3` | the most booked events |
| GET | `/events/{id}` | one event |
| POST | `/events` | create an event |
| PUT | `/events/{id}` | update an event (409 if the capacity would drop below the seats sold) |
| DELETE | `/events/{id}` | cancel an event and its bookings |
| POST | `/events/{eventId}/bookings` | book seats: 201, 404, 409 (sold out / started / lost a race), 400 |
| GET | `/events/{eventId}/bookings` | the bookings of an event |
| GET | `/events/{eventId}/bookings/{bookingId}` | one booking |
| DELETE | `/events/{eventId}/bookings/{bookingId}` | cancel a booking; the seats go back on sale |
| GET | `/events/{eventId}/bookings/summary` | bookings report (MongoDB aggregation) |

The whole API, generated from the code: http://localhost:8085/swagger-ui.html (JSON: `/v3/api-docs`).

## Architecture

```
EventController ─▶ EventService ─▶ EventRepository ─▶ MongoDB (events)
                       │ publishes EventChanged / EventCancelled (Spring application events)
                       ▼
BookingController ─▶ BookingService ─▶ BookingRepository ─▶ MongoDB (bookings)
                       │ BookingConfirmed / BookingCancelled / BookedEventChanged
                       ▼
                 BookingEventPublisher  (a port)
                       └─ RabbitBookingEventPublisher ─▶ RabbitMQ exchange "ticketing.events" (topic)
                                                              │ booking.confirmed / booking.cancelled / booked.event-changed
                                                              ▼
notification-service:  3 durable queues (+ a dead-letter queue each) ─▶ BookingEventsListener ─▶ NotificationService
                                                                        (one notification per message id: idempotent)
```

| Method | Path (notification-service, :8086) | |
| --- | --- | --- |
| GET | `/notifications?customerEmail=` | notifications, newest first; optionally one customer's |

## Run the tests

```bash
./gradlew test        # Windows PowerShell: .\gradlew.bat test
```

Gradle downloads Java 25 itself if it is not installed (toolchain). The persistence and messaging tests start their own MongoDB and RabbitMQ in Docker (Testcontainers), so Docker must be running.

## The Java tour

Each page is a `main` you can run from the IDE, or after `./gradlew :ticketing-service:compileJava`:

```bash
java -cp ticketing-service/build/classes/java/main ro.frit.features.Java8Features    # lambdas, streams, Optional, java.time
java -cp ticketing-service/build/classes/java/main ro.frit.features.Java17Features   # var, text blocks, switch expressions, records, sealed types
java -cp ticketing-service/build/classes/java/main ro.frit.features.Java21Features   # pattern switch, sequenced collections, virtual threads
java -cp ticketing-service/build/classes/java/main ro.frit.features.Java25Features   # '_', gatherers, statements before super(), scoped values
java Hello.java                                                    # a compact source file, no class needed
```

## Progress

| Session | What was added |
| --- | --- |
| 1 | Gradle project on Java 25; the ticketing domain (`TicketOffice` with a sealed `BookingResult`, tests from [docs/booking-cases.md](docs/booking-cases.md)); a tour of Java 8 → 25 in `src/main/java/ro/frit/features` and `Hello.java` |
| 2 | Spring Boot REST API for events and bookings; every `BookingResult` mapped to a status and a ProblemDetail; MockMvc tests from the case tables; HTTP request files; CI on GitHub Actions |
| 3 | Packages by feature (`event`, `booking`, `shared.error`); events and bookings in MongoDB through Spring Data repositories and services; errors as exceptions → `GlobalExceptionHandler` → ProblemDetail; validation before every save; Testcontainers tests; `DataSeeder`; `compose.yml`; `cloud` profile for Atlas |
| 4 | Search with `Criteria` + `MongoTemplate` (repository fragment); bookings report with an aggregation pipeline; indexes; optimistic locking with `@Version` + Spring 7 `@Retryable` (proved by `ConcurrentBookingTest`); a startup migration for events stored before versions existed |
| 5 | Complete API (PUT / DELETE events, list and cancel bookings); the `BookingEventPublisher` port with a logging adapter; events inside the application (`EventChanged`, `EventCancelled`) handled by `BookingService`; a unit test with the port mocked; OpenAPI + Swagger UI (springdoc) |
| 6 | Two services in one Gradle build; the RabbitMQ adapter of the port (topic exchange, JSON, message ids); notification-service with durable queues, dead-letter queues and idempotent storage; `cloud` profiles for Atlas + CloudAMQP; architecture rules with ArchUnit |
| 7 | `api-gateway` (Spring Cloud Gateway WebMVC): routes, Bucket4j rate limit per client (429 + Retry-After), one `X-Correlation-Id` through all services and RabbitMQ into every log line, API key for callers (`GATEWAY_API_KEY`) |
