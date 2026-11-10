package ro.frit.ticketing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.config.TestcontainersConfiguration;

// The whole application starts, connected to a MongoDB in Docker.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TicketingApplicationTest {

    @Test
    void theApplicationStarts() {
    }
}
