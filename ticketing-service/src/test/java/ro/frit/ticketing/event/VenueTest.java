package ro.frit.ticketing.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VenueTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void aVenueWithoutANameCannotExist(String name) {
        assertThrows(IllegalArgumentException.class, () -> new Venue(name, "Cluj-Napoca"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void aVenueWithoutACityCannotExist(String city) {
        assertThrows(IllegalArgumentException.class, () -> new Venue("Sala Polivalenta", city));
    }

    @Test
    void twoVenuesWithTheSameNameAndCityAreEqual() {
        assertEquals(new Venue("Sala Polivalenta", "Cluj-Napoca"), new Venue("Sala Polivalenta", "Cluj-Napoca"));
    }
}
