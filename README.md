# Ticketing app

[![Build](https://github.com/kaffailevi/frit-ticketing-app/actions/workflows/build.yml/badge.svg)](https://github.com/kaffailevi/frit-ticketing-app/actions/workflows/build.yml)

My version of the course project from **FutureReady IT — Module 2: modern architecture with Java and Cloud**: a small distributed ticketing system for learning events, built session by session and deployed to Google Cloud.

## Stack

Java 25 · Spring Boot 4.1 (Web MVC, Bean Validation, ProblemDetail) · MongoDB 8 with Spring Data (Docker locally, Atlas in the cloud) · Testcontainers · Gradle 9 (Kotlin DSL, version catalog) · JUnit 6 + MockMvc · GitHub Actions

## Run the API

Locally, with MongoDB in Docker:

```bash
docker compose up -d   # MongoDB on localhost:27017 (data kept in a volume)
./gradlew bootRun      # http://localhost:8085/events — the learning events are seeded on the first start
```

Against MongoDB Atlas (`cloud` profile). It reads `MONGODB_HOST` (e.g. `cluster0.abcde.mongodb.net`), `MONGODB_USERNAME` and `MONGODB_PASSWORD` from the environment, never from a file:

```bash
./gradlew bootRun --args='--spring.profiles.active=cloud'
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
                       └─ LoggingBookingEventPublisher (adapter today; RabbitMQ from session 6)
```

## Run the tests

```bash
./gradlew test        # Windows PowerShell: .\gradlew.bat test
```

Gradle downloads Java 25 itself if it is not installed (toolchain). The persistence tests start their own MongoDB in Docker (Testcontainers), so Docker must be running.

## The Java tour

Each page is a `main` you can run from the IDE, or after `./gradlew compileJava`:

```bash
java -cp build/classes/java/main ro.frit.features.Java8Features    # lambdas, streams, Optional, java.time
java -cp build/classes/java/main ro.frit.features.Java17Features   # var, text blocks, switch expressions, records, sealed types
java -cp build/classes/java/main ro.frit.features.Java21Features   # pattern switch, sequenced collections, virtual threads
java -cp build/classes/java/main ro.frit.features.Java25Features   # '_', gatherers, statements before super(), scoped values
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
