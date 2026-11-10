package ro.frit.ticketing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaVersionTest {

    @Test
    void runsOnJava25() {
        assertEquals(25, Runtime.version().feature(), "The Gradle toolchain should run the tests on Java 25");
    }
}
