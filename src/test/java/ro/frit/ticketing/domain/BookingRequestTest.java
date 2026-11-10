package ro.frit.ticketing.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingRequestTest {

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void aRequestForLessThanOneSeatCannotExist(int seats) {
        assertThrows(IllegalArgumentException.class, () -> new BookingRequest("e1", "ana@example.com", seats));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void aRequestWithoutAnEmailCannotExist(String email) {
        assertThrows(IllegalArgumentException.class, () -> new BookingRequest("e1", email, 2));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void aRequestWithoutAnEventIdCannotExist(String eventId) {
        assertThrows(IllegalArgumentException.class, () -> new BookingRequest(eventId, "ana@example.com", 2));
    }

    @Test
    void aValidRequestKeepsWhatWasAskedFor() {
        assertEquals(2, new BookingRequest("e1", "ana@example.com", 2).seats());
    }
}
