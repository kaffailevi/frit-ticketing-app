# Ticketing app

[![Build](https://github.com/kaffailevi/frit-ticketing-app/actions/workflows/build.yml/badge.svg)](https://github.com/kaffailevi/frit-ticketing-app/actions/workflows/build.yml)

My version of the course project from **FutureReady IT — Module 2: modern architecture with Java and Cloud**: a small distributed ticketing system for learning events, built session by session and deployed to Google Cloud.

## Stack

Java 25 · Spring Boot 4.1 (Web MVC, Bean Validation, ProblemDetail) · Gradle 9 (Kotlin DSL, version catalog) · JUnit 6 + MockMvc · GitHub Actions

## Run the API

```bash
./gradlew bootRun     # http://localhost:8085/events
```

Try it with the request files in [http/](http/) (IntelliJ HTTP client or VS Code REST Client). What each endpoint answers, including the errors: [docs/event-cases.md](docs/event-cases.md) and [docs/booking-cases.md](docs/booking-cases.md).

| Method | Path | |
| --- | --- | --- |
| GET | `/events?city=` | all events, or the upcoming ones in a city |
| GET | `/events/top?limit=3` | the most booked events |
| GET | `/events/{id}` | one event |
| POST | `/events` | create an event |
| POST | `/events/{eventId}/bookings` | book seats: 201, 404, 409 (sold out / started), 400 |
| GET | `/events/{eventId}/bookings/{bookingId}` | one booking |

## Run the tests

```bash
./gradlew test        # Windows PowerShell: .\gradlew.bat test
```

Gradle downloads Java 25 itself if it is not installed (toolchain).

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
