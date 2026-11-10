package ro.frit.features;

import ro.frit.ticketing.event.Event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Gatherers;

// Java 22 to 25 (2024–2025; 25 is the LTS this project runs on).
// See also Hello.java in the repository root: a whole program without a class.
public class Java25Features {

    public static void main(String[] args) throws Exception {
        unnamedVariables();
        streamGatherers();
        flexibleConstructorBodies();
        scopedValues();
    }

    // Java 22: '_' for a value you must declare but do not use.
    private static void unnamedVariables() {
        Map<String, Integer> seats = Map.of("e1", 9, "e3", 88);
        AtomicInteger count = new AtomicInteger();
        seats.forEach((_, free) -> count.addAndGet(free));
        try {
            Integer.parseInt("not a number");
        } catch (NumberFormatException _) {
            System.out.println("Unnamed variables: the exception is not used, only caught; free seats " + count);
        }
    }

    // Java 24: gatherers are custom intermediate steps for streams. The JDK ships five.
    private static void streamGatherers() throws Exception {
        List<Event> events = TourData.events();

        // windowFixed: groups with no overlap (e.g. 2 reminder emails per call).
        System.out.println("windowFixed(2): " + events.stream().map(Event::getId).gather(Gatherers.windowFixed(2)).toList());

        // windowSliding: each element with its neighbour (e.g. compare consecutive events).
        System.out.println("windowSliding(2): " + events.stream().map(Event::getId).gather(Gatherers.windowSliding(2)).toList());

        // scan: a running total, one output per input. fold: only the final result.
        System.out.println("scan (seats sold so far): " + events.stream()
                .gather(Gatherers.scan(() -> 0, (total, event) -> total + event.getBookedSeats())).toList());
        System.out.println("fold (all seats sold): " + events.stream()
                .gather(Gatherers.fold(() -> 0, (total, event) -> total + event.getBookedSeats())).findFirst().orElse(0));

        // mapConcurrent: map with up to N calls at once, on virtual threads; the order is kept.
        long start = System.nanoTime();
        List<String> prices = events.stream().gather(Gatherers.mapConcurrent(4, Java25Features::slowPriceLookup)).toList();
        System.out.printf("mapConcurrent: %s in %d ms (4 lookups of 300 ms each)%n", prices, (System.nanoTime() - start) / 1_000_000);
    }

    private static String slowPriceLookup(Event event) {
        try {
            Thread.sleep(300); // stands in for a call to another service
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        return event.getId() + "=" + (event.getCapacity() * 2) + " RON";
    }

    // Java 25: statements before super(...). Check and prepare the arguments first,
    // so an invalid object never reaches the parent constructor. 'this' cannot be used yet.
    static class Person {
        final String email;

        Person(String email) {
            this.email = email;
        }
    }

    static class Customer extends Person {
        Customer(String email) {
            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("Not an email: " + email);
            }
            super(email.strip().toLowerCase());
        }
    }

    private static void flexibleConstructorBodies() {
        System.out.println("Flexible constructor: " + new Customer("  Ana@Example.com ").email);
        try {
            new Customer("ana");
        } catch (IllegalArgumentException refused) {
            System.out.println("  refused before super(): " + refused.getMessage());
        }
    }

    // Java 25: a ScopedValue is a value visible to everything called inside run(), and only there.
    // The modern replacement for ThreadLocal, e.g. for a request's correlation id (session 7).
    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    private static void scopedValues() {
        ScopedValue.where(REQUEST_ID, "req-42").run(() -> logBooking("e1"));
        System.out.println("  outside run(): bound = " + REQUEST_ID.isBound());
    }

    private static void logBooking(String eventId) {
        System.out.println("Scoped value: [" + REQUEST_ID.get() + "] booking " + eventId);
    }
}
