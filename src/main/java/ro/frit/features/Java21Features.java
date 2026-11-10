package ro.frit.features;

import ro.frit.ticketing.domain.Booking;
import ro.frit.ticketing.domain.BookingResult;
import ro.frit.ticketing.domain.Event;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

// Java 21 (2023, LTS): data-oriented programming and cheap threads.
public class Java21Features {

    public static void main(String[] args) throws InterruptedException {
        patternsInSwitch();
        sequencedCollections();
        virtualThreads();
    }

    // Each case is a type; a record pattern takes the record apart right in the label; a guard (when) adds a condition.
    // No default: BookingResult is sealed, so the compiler checks that every outcome is handled.
    private static void patternsInSwitch() {
        Booking booking = Booking.builder().id("b1").eventId("e1").customerEmail("ana@example.com").seats(6).build();
        List<BookingResult> results = List.of(
                new BookingResult.Confirmed(booking),
                new BookingResult.SoldOut("e2", 0),
                new BookingResult.SoldOut("e3", 2),
                new BookingResult.UnknownEvent("e9"),
                new BookingResult.AlreadyStarted("e4", LocalDateTime.of(2026, 11, 29, 18, 0)));

        for (BookingResult result : results) {
            String advice = switch (result) {
                case BookingResult.Confirmed(Booking b) when b.getSeats() > 5 -> "group booking, send one invoice";
                case BookingResult.Confirmed(Booking b) -> "send " + b.getSeats() + " ticket(s)";
                case BookingResult.SoldOut(String eventId, int left) when left == 0 -> eventId + ": add to waiting list";
                case BookingResult.SoldOut(String eventId, int left) -> eventId + ": offer the " + left + " seat(s) left";
                case BookingResult.UnknownEvent(String eventId) -> "check the id " + eventId;
                case BookingResult.AlreadyStarted(String eventId, LocalDateTime startsAt) -> "suggest the recording of " + eventId;
            };
            System.out.println("Pattern switch: " + advice);
        }
    }

    // Lists, deques, LinkedHashMap and LinkedHashSet share one interface for "has an order":
    // getFirst(), getLast(), reversed().
    private static void sequencedCollections() {
        List<Event> events = new ArrayList<>(TourData.events());
        SequencedMap<String, Integer> seatsById = new LinkedHashMap<>();
        events.forEach(event -> seatsById.put(event.getId(), event.availableSeats()));

        System.out.println("Sequenced: first " + events.getFirst().getName() + ", last " + events.getLast().getName()
                + ", reversed ids " + seatsById.reversed().keySet() + ", first entry " + seatsById.firstEntry());
    }

    // 2,000 tasks that each wait 200 ms, like a call to a database or another service.
    // A platform thread is an OS thread, expensive, so pools are small. A virtual thread is cheap:
    // while it waits, its OS thread serves other virtual threads.
    private static void virtualThreads() throws InterruptedException {
        long platform = runTasks(Executors.newFixedThreadPool(200));
        long virtual = runTasks(Executors.newVirtualThreadPerTaskExecutor());
        System.out.printf("Virtual threads: 200 platform threads took %d ms, virtual threads took %d ms%n", platform, virtual);
    }

    private static long runTasks(ExecutorService executor) throws InterruptedException {
        long start = System.nanoTime();
        try (executor) { // Java 19+: an ExecutorService is AutoCloseable; close() waits for all tasks
            IntStream.range(0, 2_000).forEach(i -> executor.submit(() -> {
                Thread.sleep(Duration.ofMillis(200));
                return i;
            }));
        }
        return Duration.ofNanos(System.nanoTime() - start).toMillis();
    }
}
