package ro.frit.ticketing.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.frit.ticketing.domain.Event;
import ro.frit.ticketing.domain.TicketOffice;
import ro.frit.ticketing.domain.Venue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// One test per row of docs/event-cases.md.
@WebMvcTest(EventController.class)
class EventControllerTest {

    private static final Event REACT = Event.builder()
            .id("react-cluj")
            .name("React Workshop")
            .venue(new Venue("Cluj Innovation Park", "Cluj-Napoca"))
            .startsAt(LocalDateTime.of(2030, 2, 10, 18, 0))
            .capacity(30)
            .bookedSeats(12)
            .build();

    private static final Event CLOUD = REACT.toBuilder()
            .id("cloud-iasi").name("Cloud Run Hands-on").venue(new Venue("Palas Tech Hub", "Iasi")).build();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketOffice ticketOffice;

    @Test
    void allEventsAreListed() throws Exception {
        when(ticketOffice.allEvents()).thenReturn(List.of(REACT, CLOUD));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("react-cluj"))
                .andExpect(jsonPath("$[0].venueName").value("Cluj Innovation Park"))
                .andExpect(jsonPath("$[0].city").value("Cluj-Napoca"))
                .andExpect(jsonPath("$[0].startsAt").value("2030-02-10T18:00:00"))
                .andExpect(jsonPath("$[0].capacity").value(30))
                .andExpect(jsonPath("$[0].availableSeats").value(18));
    }

    @Test
    void aCityListsOnlyItsUpcomingEvents() throws Exception {
        when(ticketOffice.upcomingEventsIn("Iasi")).thenReturn(List.of(CLOUD));

        mockMvc.perform(get("/events").param("city", "Iasi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("cloud-iasi"));
    }

    @Test
    void theTopEventsAreTheThreeMostBookedByDefault() throws Exception {
        when(ticketOffice.topEventsByBookedSeats(3)).thenReturn(List.of(REACT));

        mockMvc.perform(get("/events/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("react-cluj"));
    }

    @Test
    void aTopListOfZeroEventsIsABadRequest() throws Exception {
        mockMvc.perform(get("/events/top").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }

    @Test
    void anExistingEventIsReturned() throws Exception {
        when(ticketOffice.findEvent("react-cluj")).thenReturn(Optional.of(REACT));

        mockMvc.perform(get("/events/react-cluj"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("React Workshop"));
    }

    @Test
    void anUnknownEventIsNotFound() throws Exception {
        when(ticketOffice.findEvent("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/events/nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No event with id nope"));
    }

    @Test
    void aValidEventIsCreatedWithALocationHeader() throws Exception {
        when(ticketOffice.addEvent(any())).thenAnswer(call -> call.getArgument(0));

        mockMvc.perform(post("/events").contentType(APPLICATION_JSON).content("""
                        {"name": "Folk Evening", "venueName": "Sala Polivalenta", "city": "Cluj-Napoca",
                         "startsAt": "2030-10-01T19:00:00", "capacity": 300}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("http://localhost/events/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.name").value("Folk Evening"))
                .andExpect(jsonPath("$.availableSeats").value(300));
    }

    @Test
    void anInvalidEventIsABadRequestAndNeverReachesTheOffice() throws Exception {
        mockMvc.perform(post("/events").contentType(APPLICATION_JSON).content("""
                        {"name": " ", "venueName": "Sala Polivalenta", "city": "Cluj-Napoca",
                         "startsAt": "2020-10-01T19:00:00", "capacity": 0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));

        verify(ticketOffice, never()).addEvent(any());
    }
}
