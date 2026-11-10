package ro.frit.ticketing.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mongodb.MongoDBContainer;

// A real MongoDB in Docker for the tests, the same version as compose.yml. @ServiceConnection gives its
// address to Spring, so no spring.mongodb.* property is needed. Static: one container for all test classes.
@TestConfiguration
@ImportTestcontainers
public class TestcontainersConfiguration {

    @ServiceConnection
    static MongoDBContainer mongoDB = new MongoDBContainer("mongo:8.0");
}
