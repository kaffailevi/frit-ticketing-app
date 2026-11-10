package ro.frit.ticketing;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import ro.frit.ticketing.booking.BookingEventPublisher;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

// The architecture rules of sessions 3–6, written as tests: the build fails the moment one is broken,
// instead of someone noticing in a review. ArchUnit reads the compiled classes and their dependencies.
@AnalyzeClasses(packages = "ro.frit.ticketing", importOptions = DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule onlyTheAdapterKnowsTheBroker = noClasses()
            .that().doNotImplement(BookingEventPublisher.class)
            .and().resideOutsideOfPackage("..config..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.amqp..")
            .because("the booking rules say what they need through the port; RabbitMQ is the adapter's business");

    @ArchTest
    static final ArchRule eventsDoNotKnowBookings = noClasses()
            .that().resideInAPackage("..ticketing.event..")
            .should().dependOnClassesThat().resideInAPackage("..ticketing.booking..")
            .because("booking asks the event feature; events announce changes and let bookings listen");

    @ArchTest
    static final ArchRule controllersDoNotUseRepositories = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
            .because("a controller talks to its service; the service owns the data");

    @ArchTest
    static final ArchRule theServiceDoesNotUseTheJavaTour = noClasses()
            .that().resideInAPackage("ro.frit.ticketing..")
            .should().dependOnClassesThat().resideInAPackage("ro.frit.features..")
            .because("the Java tour is learning material, not part of the service");
}
