# Ticketing app

My version of the course project from **FutureReady IT — Module 2: modern architecture with Java and Cloud**: a small distributed ticketing system for learning events, built session by session and deployed to Google Cloud.

## Stack

Java 25 · Gradle 9 (Kotlin DSL, version catalog) · JUnit 6

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
