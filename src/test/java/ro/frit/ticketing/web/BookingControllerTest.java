package ro.frit.ticketing.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.frit.ticketing.domain.Booking;
import ro.frit.ticketing.domain.BookingRequest;
import ro.frit.ticketing.domain.BookingResult;
import ro.frit.ticketing.domain.TicketOffice;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// One test per row of the HTTP section of docs/booking-cases.md.
@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String VALID_BODY = """
            {"customerEmail": "ana@example.com", "seats": 2}""";
    private static final BookingRequest VALID_REQUEST = new BookingRequest("e1", "ana@example.com", 2);
    private static final Booking BOOKING = Booking.builder()
            .id("b1")
            .eventId("e1")
            .customerEmail("ana@example.com")
            .seats(2)
            .bookedAt(LocalDateTime.of(2030, 1, 5, 10, 30))
            .build();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketOffice ticketOffice;

    private ResultActions book(String eventId, String body) throws Exception {
        return mockMvc.perform(post("/events/{eventId}/bookings", eventId).contentType(APPLICATION_JSON).content(body));
    }

    private void expectBadRequestWithoutCallingTheOffice(String body) throws Exception {
        book("e1", body)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
        verify(ticketOffice, never()).book(any());
    }

    @Test
    void aConfirmedBookingIsCreatedWithALocationToReadIt() throws Exception {
        when(ticketOffice.book(VALID_REQUEST)).thenReturn(new BookingResult.Confirmed(BOOKING));

        book("e1", VALID_BODY)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/events/e1/bookings/b1"))
                .andExpect(jsonPath("$.id").value("b1"))
                .andExpect(jsonPath("$.eventId").value("e1"))
                .andExpect(jsonPath("$.customerEmail").value("ana@example.com"))
                .andExpect(jsonPath("$.seats").value(2))
                .andExpect(jsonPath("$.bookedAt").value("2030-01-05T10:30:00"));
    }

    @Test
    void theEventIdComesFromThePathNotFromTheBody() throws Exception {
        when(ticketOffice.book(VALID_REQUEST)).thenReturn(new BookingResult.Confirmed(BOOKING));

        book("e1", """
                {"eventId": "other", "customerEmail": "ana@example.com", "seats": 2}""")
                .andExpect(status().isCreated());

        verify(ticketOffice).book(VALID_REQUEST);
    }

    @Test
    void anUnknownEventIsNotFound() throws Exception {
        when(ticketOffice.book(new BookingRequest("e9", "ana@example.com", 2)))
                .thenReturn(new BookingResult.UnknownEvent("e9"));

        book("e9", VALID_BODY)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No event with id e9"));
    }

    @Test
    void notEnoughSeatsIsAConflictThatSaysHowManyAreLeft() throws Exception {
        when(ticketOffice.book(VALID_REQUEST)).thenReturn(new BookingResult.SoldOut("e1", 1));

        book("e1", VALID_BODY)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.availableSeats").value(1));
    }

    @Test
    void anEventThatHasStartedIsAConflictThatSaysWhenItStarted() throws Exception {
        when(ticketOffice.book(VALID_REQUEST))
                .thenReturn(new BookingResult.AlreadyStarted("e1", LocalDateTime.of(2026, 11, 1, 18, 0)));

        book("e1", VALID_BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.startsAt").value("2026-11-01T18:00:00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "not-an-email"})
    void aBlankOrInvalidEmailIsABadRequest(String email) throws Exception {
        expectBadRequestWithoutCallingTheOffice("""
                {"customerEmail": "%s", "seats": 2}""".formatted(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"customerEmail\": \"ana@example.com\", \"seats\": 0}",
            "{\"customerEmail\": \"ana@example.com\", \"seats\": -1}",
            "{\"customerEmail\": \"ana@example.com\"}"})
    void zeroNegativeOrMissingSeatsAreABadRequest(String body) throws Exception {
        expectBadRequestWithoutCallingTheOffice(body);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"customerEmail\": ", "{\"customerEmail\": \"ana@example.com\", \"seats\": \"two\"}"})
    void brokenJsonOrAWrongFieldTypeIsABadRequest(String body) throws Exception {
        expectBadRequestWithoutCallingTheOffice(body);
    }

    @Test
    void aBodyThatIsNotJsonIsAnUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(TEXT_PLAIN).content("2 seats please"))
                .andExpect(status().isUnsupportedMediaType());

        verify(ticketOffice, never()).book(any());
    }

    @Test
    void aBookingCanBeReadUnderItsEvent() throws Exception {
        when(ticketOffice.findBooking("b1")).thenReturn(Optional.of(BOOKING));

        mockMvc.perform(get("/events/e1/bookings/b1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerEmail").value("ana@example.com"));
    }

    @Test
    void aBookingIsNotVisibleThroughAnotherEventsUrl() throws Exception {
        when(ticketOffice.findBooking("b1")).thenReturn(Optional.of(BOOKING));

        mockMvc.perform(get("/events/e2/bookings/b1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }
}
