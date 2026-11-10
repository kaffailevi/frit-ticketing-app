package ro.frit.ticketing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ro.frit.ticketing.config.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The API description is generated from the running application and documents the booking errors.
@SpringBootTest(properties = "spring.mongodb.database=openapi-test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theApiDescriptionListsTheBookingEndpointWithItsErrors() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Ticketing API"))
                .andExpect(jsonPath("$.paths['/events/{eventId}/bookings'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/events'].get.parameters[?(@.name == 'city')]").exists());
    }
}
