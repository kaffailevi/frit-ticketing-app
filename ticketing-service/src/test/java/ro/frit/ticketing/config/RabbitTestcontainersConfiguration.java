package ro.frit.ticketing.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.rabbitmq.RabbitMQContainer;

// A real RabbitMQ in Docker, the same image as compose.yml. Separate from the MongoDB one, so the persistence tests
// do not start a broker they never use.
@TestConfiguration
@ImportTestcontainers
public class RabbitTestcontainersConfiguration {

    @ServiceConnection
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:4-management");
}
