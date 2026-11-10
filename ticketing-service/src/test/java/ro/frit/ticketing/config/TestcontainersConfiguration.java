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

    // GLIBC_TUNABLES: MongoDB 8.0 refuses to start on Linux kernel 6.19+ (SERVER-121912), which GitHub's newer
    // ubuntu runners use; this setting lets it start there. It changes nothing on Docker Desktop.
    @ServiceConnection
    static MongoDBContainer mongoDB = new MongoDBContainer("mongo:8.0").withEnv("GLIBC_TUNABLES", "glibc.pthread.rseq=1");
}
