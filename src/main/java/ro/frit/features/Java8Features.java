package ro.frit.features;

import ro.frit.ticketing.event.Event;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// Java 8 (2014): the release that made Java functional. Run main() and read the output next to the code.
public class Java8Features {

    public static void main(String[] args) {
        lambdas();
        methodReferences();
        streams();
        collectors();
        optional();
        timeApi();
    }

    // A lambda is a function you can pass around. It fits any interface with exactly one abstract method.
    private static void lambdas() {
        Predicate<Event> hasFreeSeats = event -> event.availableSeats() > 0;
        Function<Event, String> label = event -> event.getName() + " (" + event.getVenue().city() + ")";
        Supplier<String> greeting = () -> "Lambdas:";

        System.out.println(greeting.get());
        TourData.events().stream()
                .filter(hasFreeSeats.and(event -> event.getCapacity() >= 40))
                .map(label)
                .forEach(text -> System.out.println("  " + text));
    }

    // A method reference is a lambda that only calls one method: Event::getName means event -> event.getName().
    private static void methodReferences() {
        List<String> names = TourData.events().stream().map(Event::getName).sorted().toList();
        System.out.println("Method references: " + names);
    }

    // A stream describes WHAT to compute (filter, map, sort); the loop is written for you.
    // Nothing runs until a terminal operation (count, toList, forEach) asks for a result.
    private static void streams() {
        long soldOut = TourData.events().stream().filter(event -> event.availableSeats() == 0).count();
        int freeSeats = TourData.events().stream().mapToInt(Event::availableSeats).sum();
        Optional<Event> fullest = TourData.events().stream().max(Comparator.comparingInt(Event::getBookedSeats));
        boolean anyInIasi = TourData.events().stream().anyMatch(event -> event.getVenue().city().equals("Iasi"));

        System.out.printf("Streams: %d sold out, %d free seats, fullest: %s, any in Iasi: %b%n",
                soldOut, freeSeats, fullest.map(Event::getName).orElse("-"), anyInIasi);
    }

    // Collectors turn a stream into a map, a string, a summary.
    private static void collectors() {
        Map<String, List<String>> namesByCity = TourData.events().stream()
                .collect(Collectors.groupingBy(event -> event.getVenue().city(),
                        Collectors.mapping(Event::getName, Collectors.toList())));
        String all = TourData.events().stream().map(Event::getId).collect(Collectors.joining(", ", "[", "]"));

        System.out.println("Collectors: " + namesByCity + " " + all);
    }

    // Optional says "there may be no value" in the type, instead of returning null.
    private static void optional() {
        Optional<Event> inTimisoara = TourData.events().stream()
                .filter(event -> event.getVenue().city().equals("Timisoara"))
                .findFirst();

        String name = inTimisoara.map(Event::getName).orElse("no event in Timisoara");
        System.out.println("Optional: " + name + ", present: " + inTimisoara.isPresent());
        // inTimisoara.get() would throw NoSuchElementException: always say what happens when it is empty.
    }

    // java.time: immutable dates and times, one type for each idea.
    private static void timeApi() {
        LocalDateTime start = TourData.NOW.plusDays(3).withHour(18);
        Duration untilStart = Duration.between(TourData.NOW, start);
        Period untilNewYear = Period.between(LocalDate.of(2026, 11, 30), LocalDate.of(2027, 1, 1));
        ZonedDateTime inBucharest = start.atZone(ZoneId.of("Europe/Bucharest"));

        System.out.printf("java.time: starts %s, in %d hours, New Year in %s, as instant %s%n",
                start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                untilStart.toHours(), untilNewYear, inBucharest.toInstant());
    }
}
