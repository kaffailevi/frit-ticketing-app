package ro.frit.ticketing.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.frit.ticketing.event.dto.CreateEventRequest;
import ro.frit.ticketing.event.dto.EventResponse;
import ro.frit.ticketing.event.dto.EventSearchRequest;
import ro.frit.ticketing.event.dto.UpdateEventRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// One test per row of docs/event-cases.md. The service is a mock: these tests check only the HTTP side.
@WebMvcTest(EventController.class)
class EventControllerTest {

    private static final EventResponse REACT = new EventResponse("react-cluj", "React Workshop", "Cluj Innovation Park",
            "Cluj-Napoca", LocalDateTime.of(2030, 2, 10, 18, 0), 30, 18);
    private static final EventResponse CLOUD = new EventResponse("cloud-iasi", "Cloud Run Hands-on", "Palas Tech Hub",
            "Iasi", LocalDateTime.of(2030, 3, 5, 10, 0), 40, 40);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Test
    void withoutFiltersTheUpcomingEventsAreListed() throws Exception {
        when(eventService.search(EventSearchRequest.none())).thenReturn(List.of(REACT, CLOUD));

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
    void everyFilterIsPassedToTheSearch() throws Exception {
        EventSearchRequest filter = new EventSearchRequest("Iasi", "cloud",
                LocalDateTime.of(2030, 3, 1, 0, 0), LocalDateTime.of(2030, 4, 1, 0, 0));
        when(eventService.search(filter)).thenReturn(List.of(CLOUD));

        mockMvc.perform(get("/events").param("city", "Iasi").param("name", "cloud")
                        .param("from", "2030-03-01T00:00:00").param("to", "2030-04-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("cloud-iasi"));
    }

    @Test
    void aDateThatIsNotIsoIsABadRequest() throws Exception {
        mockMvc.perform(get("/events").param("from", "next week"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }

    @Test
    void theTopEventsAreTheThreeMostBookedByDefault() throws Exception {
        when(eventService.findMostBooked(3)).thenReturn(List.of(REACT));

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
        when(eventService.findById("react-cluj")).thenReturn(REACT);

        mockMvc.perform(get("/events/react-cluj"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("React Workshop"));
    }

    @Test
    void anUnknownEventIsNotFound() throws Exception {
        when(eventService.findById("nope")).thenThrow(new EventNotFoundException("nope"));

        mockMvc.perform(get("/events/nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No event with id nope"));
    }

    @Test
    void aValidEventIsCreatedWithALocationHeader() throws Exception {
        when(eventService.create(any(CreateEventRequest.class))).thenReturn(CLOUD);

        mockMvc.perform(post("/events").contentType(APPLICATION_JSON).content("""
                        {"name": "Cloud Run Hands-on", "venueName": "Palas Tech Hub", "city": "Iasi",
                         "startsAt": "2030-03-05T10:00:00", "capacity": 40}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/events/cloud-iasi"))
                .andExpect(jsonPath("$.availableSeats").value(40));
    }

    @Test
    void anInvalidEventIsABadRequestAndNeverReachesTheService() throws Exception {
        mockMvc.perform(post("/events").contentType(APPLICATION_JSON).content("""
                        {"name": " ", "venueName": "Sala Polivalenta", "city": "Cluj-Napoca",
                         "startsAt": "2020-10-01T19:00:00", "capacity": 0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));

        verify(eventService, never()).create(any());
    }

    private static final String UPDATE_BODY = """
            {"name": "Cloud Run Deep Dive", "venueName": "Palas Tech Hub", "city": "Iasi",
             "startsAt": "2030-03-05T10:00:00", "capacity": 50}""";

    @Test
    void anEventIsUpdatedWithPut() throws Exception {
        when(eventService.update(eq("cloud-iasi"), any(UpdateEventRequest.class))).thenReturn(CLOUD);

        mockMvc.perform(put("/events/cloud-iasi").contentType(APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cloud-iasi"));
    }

    @Test
    void anUpdateBelowTheSoldSeatsIsAConflict() throws Exception {
        when(eventService.update(eq("cloud-iasi"), any(UpdateEventRequest.class)))
                .thenThrow(new CapacityBelowSoldSeatsException("cloud-iasi", 60));

        mockMvc.perform(put("/events/cloud-iasi").contentType(APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.bookedSeats").value(60));
    }

    @Test
    void anInvalidUpdateIsABadRequest() throws Exception {
        mockMvc.perform(put("/events/cloud-iasi").contentType(APPLICATION_JSON).content("""
                        {"name": "", "venueName": "Hub", "city": "Iasi", "startsAt": "2030-03-05T10:00:00"}"""))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).update(any(), any());
    }

    @Test
    void aCancelledEventAnswersNoContent() throws Exception {
        mockMvc.perform(delete("/events/cloud-iasi"))
                .andExpect(status().isNoContent());

        verify(eventService).cancel("cloud-iasi");
    }

    @Test
    void cancellingAnUnknownEventIsNotFound() throws Exception {
        doThrow(new EventNotFoundException("nope")).when(eventService).cancel("nope");

        mockMvc.perform(delete("/events/nope"))
                .andExpect(status().isNotFound());
    }
}
