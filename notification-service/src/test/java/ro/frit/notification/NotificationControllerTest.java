package ro.frit.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    private static final Notification ANAS = new Notification("n1", "m-1", "b1", "ana@example.com",
            "2 seat(s) for React Workshop on 10 Feb 2027 at 18:00 are confirmed.", LocalDateTime.of(2026, 11, 30, 12, 0));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void allNotificationsAreListed() throws Exception {
        when(notificationService.findAll(null)).thenReturn(List.of(ANAS));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value(ANAS.getText()));
    }

    @Test
    void oneCustomersNotificationsAreListedByEmail() throws Exception {
        when(notificationService.findAll("ana@example.com")).thenReturn(List.of(ANAS));

        mockMvc.perform(get("/notifications").param("customerEmail", "ana@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerEmail").value("ana@example.com"));
    }
}
