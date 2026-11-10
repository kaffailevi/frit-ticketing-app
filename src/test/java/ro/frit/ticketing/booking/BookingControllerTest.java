package ro.frit.ticketing.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.frit.ticketing.booking.dto.BookingResponse;
import ro.frit.ticketing.booking.dto.BookingSummaryResponse;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.EventAlreadyStartedException;
import ro.frit.ticketing.event.EventNotFoundException;
import ro.frit.ticketing.event.NotEnoughSeatsException;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

// One test per row of the HTTP section of docs/booking-cases.md. The service is a mock; its exceptions
// travel through GlobalExceptionHandler, which @WebMvcTest loads together with the controller.
@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String VALID_BODY = """
            {"customerEmail": "ana@example.com", "seats": 2}""";
    private static final CreateBookingRequest VALID_REQUEST = new CreateBookingRequest("ana@example.com", 2);
    private static final BookingResponse BOOKING =
            new BookingResponse("b1", "e1", "ana@example.com", 2, LocalDateTime.of(2030, 1, 5, 10, 30));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    private ResultActions book(String eventId, String body) throws Exception {
        return mockMvc.perform(post("/events/{eventId}/bookings", eventId).contentType(APPLICATION_JSON).content(body));
    }

    private void expectBadRequestWithoutCallingTheService(String body) throws Exception {
        book("e1", body)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
        verify(bookingService, never()).book(anyString(), any());
    }

    @Test
    void aConfirmedBookingIsCreatedWithALocationToReadIt() throws Exception {
        when(bookingService.book("e1", VALID_REQUEST)).thenReturn(BOOKING);

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
        when(bookingService.book("e1", VALID_REQUEST)).thenReturn(BOOKING);

        book("e1", """
                {"eventId": "other", "customerEmail": "ana@example.com", "seats": 2}""")
                .andExpect(status().isCreated());

        verify(bookingService).book("e1", VALID_REQUEST);
    }

    @Test
    void anUnknownEventIsNotFound() throws Exception {
        when(bookingService.book("e9", VALID_REQUEST)).thenThrow(new EventNotFoundException("e9"));

        book("e9", VALID_BODY)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No event with id e9"));
    }

    @Test
    void notEnoughSeatsIsAConflictThatSaysHowManyAreLeft() throws Exception {
        when(bookingService.book("e1", VALID_REQUEST)).thenThrow(new NotEnoughSeatsException("e1", 1));

        book("e1", VALID_BODY)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.availableSeats").value(1));
    }

    @Test
    void anEventThatHasStartedIsAConflictThatSaysWhenItStarted() throws Exception {
        when(bookingService.book("e1", VALID_REQUEST))
                .thenThrow(new EventAlreadyStartedException("e1", LocalDateTime.of(2026, 11, 1, 18, 0)));

        book("e1", VALID_BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.startsAt").value("2026-11-01T18:00:00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "not-an-email"})
    void aBlankOrInvalidEmailIsABadRequest(String email) throws Exception {
        expectBadRequestWithoutCallingTheService("""
                {"customerEmail": "%s", "seats": 2}""".formatted(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"customerEmail\": \"ana@example.com\", \"seats\": 0}",
            "{\"customerEmail\": \"ana@example.com\", \"seats\": -1}",
            "{\"customerEmail\": \"ana@example.com\"}"})
    void zeroNegativeOrMissingSeatsAreABadRequest(String body) throws Exception {
        expectBadRequestWithoutCallingTheService(body);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"customerEmail\": ", "{\"customerEmail\": \"ana@example.com\", \"seats\": \"two\"}"})
    void brokenJsonOrAWrongFieldTypeIsABadRequest(String body) throws Exception {
        expectBadRequestWithoutCallingTheService(body);
    }

    @Test
    void aBodyThatIsNotJsonIsAnUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/events/e1/bookings").contentType(TEXT_PLAIN).content("2 seats please"))
                .andExpect(status().isUnsupportedMediaType());

        verify(bookingService, never()).book(anyString(), any());
    }

    @Test
    void aBookingCanBeReadUnderItsEvent() throws Exception {
        when(bookingService.findById("e1", "b1")).thenReturn(BOOKING);

        mockMvc.perform(get("/events/e1/bookings/b1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerEmail").value("ana@example.com"));
    }

    @Test
    void aBookingIsNotVisibleThroughAnotherEventsUrl() throws Exception {
        when(bookingService.findById("e2", "b1")).thenThrow(new BookingNotFoundException("e2", "b1"));

        mockMvc.perform(get("/events/e2/bookings/b1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }

    @Test
    void theSummaryOfAnEventIsReturned() throws Exception {
        when(bookingService.summaryFor("e1")).thenReturn(new BookingSummaryResponse("e1", 3, 6, 2.0));

        mockMvc.perform(get("/events/e1/bookings/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings").value(3))
                .andExpect(jsonPath("$.seats").value(6))
                .andExpect(jsonPath("$.averageSeatsPerBooking").value(2.0));
    }

    @Test
    void theSummaryOfAnUnknownEventIsNotFound() throws Exception {
        when(bookingService.summaryFor("e9")).thenThrow(new EventNotFoundException("e9"));

        mockMvc.perform(get("/events/e9/bookings/summary"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }
}
