package ro.frit.ticketing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ro.frit.ticketing.domain.TicketOffice;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class TicketingApplicationTest {

    @Autowired
    private TicketOffice ticketOffice;

    @Test
    void theTicketOfficeStartsWithTheSeedEventsFromTheConfiguration() {
        assertEquals("Spring Boot 4 Meetup", ticketOffice.findEvent("spring-cluj").orElseThrow().getName());
    }
}
