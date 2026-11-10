package ro.frit.features;

import ro.frit.ticketing.event.Event;
import ro.frit.ticketing.event.Venue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Java 9 to 17 (2017–2021): less boilerplate. Java 17 is the LTS that collects all of it.
public class Java17Features {

    public static void main(String[] args) {
        collectionFactories();
        optionalAdditions();
        localVariableTypeInference();
        strings();
        switchExpressions();
        textBlocks();
        records();
        instanceofPatterns();
        sealedTypes();
        helpfulNullPointerExceptions();
    }

    // Java 9: an immutable collection in one call. add() on it throws UnsupportedOperationException.
    private static void collectionFactories() {
        List<String> cities = List.of("Cluj-Napoca", "Brasov", "Iasi");
        Map<String, Integer> capacity = Map.of("e1", 40, "e2", 20);
        System.out.println("Collection factories: " + cities + " " + capacity);
    }

    // Java 9: ifPresentOrElse and or(). Java 10: orElseThrow() without arguments, the honest name for get().
    private static void optionalAdditions() {
        Optional<Event> first = TourData.events().stream().findFirst();
        first.ifPresentOrElse(
                event -> System.out.println("Optional additions: first is " + event.getName()),
                () -> System.out.println("Optional additions: no events"));
        System.out.println("  orElseThrow: " + first.orElseThrow().getId());
    }

    // Java 10: var. The type is still fixed at compile time, it is only not written twice.
    private static void localVariableTypeInference() {
        var events = new ArrayList<Event>(TourData.events());
        // events.add("a string");   // does not compile: events is an ArrayList<Event>
        for (var event : events) {
            if (event.availableSeats() == 0) {
                System.out.println("var: " + event.getName() + " is full");
            }
        }
    }

    // Java 11: the String methods everyone used to write by hand.
    private static void strings() {
        String input = "   ana@example.com  ";
        System.out.println("Strings: [" + input.strip() + "] blank=" + "   ".isBlank()
                + " " + "-".repeat(10) + " lines=" + "a\nb\nc".lines().count());
    }

    // Java 14: switch as an expression. Arrows do not fall through; the switch gives back a value.
    private static void switchExpressions() {
        for (Event event : TourData.events()) {
            String status = switch (event.availableSeats()) {
                case 0 -> "sold out";
                case 1, 2, 3, 4, 5 -> "last seats";
                default -> {
                    int percent = 100 * event.getBookedSeats() / event.getCapacity();
                    yield percent + "% booked";
                }
            };
            System.out.println("Switch expression: " + event.getName() + " -> " + status);
        }
    }

    // Java 15: a string over several lines, written as it looks. Indentation up to the closing quotes is removed.
    private static void textBlocks() {
        String json = """
                {
                  "eventId": "e1",
                  "seats": 2
                }
                """;
        System.out.print("Text block:\n" + json);
    }

    // Java 16: a record is a class for data. Constructor, accessors, equals, hashCode and toString are generated.
    // Venue is one; its compact constructor refuses invalid values (see VenueTest).
    private static void records() {
        Venue one = new Venue("Cluj Innovation Park", "Cluj-Napoca");
        Venue same = new Venue("Cluj Innovation Park", "Cluj-Napoca");
        System.out.println("Records: " + one + " equals the same values: " + one.equals(same));
        try {
            new Venue("Cluj Innovation Park", " ");
        } catch (IllegalArgumentException refused) {
            System.out.println("  refused at creation: " + refused.getMessage());
        }
    }

    // Java 16: check the type and cast in one step.
    private static void instanceofPatterns() {
        Object something = TourData.events().getFirst();
        if (something instanceof Event event && event.availableSeats() > 0) {
            System.out.println("instanceof pattern: " + event.getName() + " has free seats");
        }
    }

    // Java 17: a sealed type lists every allowed subtype. See BookingResult: four outcomes, no others possible.
    sealed interface Ticket permits Standard, Student {
    }

    record Standard(int price) implements Ticket {
    }

    record Student(int price, String university) implements Ticket {
    }

    // A class outside the permits list cannot implement Ticket: "class Vip implements Ticket" does not compile.
    private static void sealedTypes() {
        List<String> allowed = Arrays.stream(Ticket.class.getPermittedSubclasses()).map(Class::getSimpleName).toList();
        System.out.println("Sealed: the only kinds of Ticket are " + allowed);
    }

    // Java 14: a NullPointerException says which variable was null, not only the line.
    private static void helpfulNullPointerExceptions() {
        Event event = Event.builder().id("e9").name("No venue yet").build();
        try {
            System.out.println(event.getVenue().city());
        } catch (NullPointerException exception) {
            System.out.println("Helpful NPE: " + exception.getMessage());
        }
    }
}
