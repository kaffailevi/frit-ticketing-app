package ro.frit.notification.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

// A MongoDB and a RabbitMQ in Docker, the same images as compose.yml.
// GLIBC_TUNABLES: MongoDB 8.0 refuses to start on Linux kernel 6.19+ (SERVER-121912), as on GitHub's newer runners.
@TestConfiguration
@ImportTestcontainers
public class TestcontainersConfiguration {

    @ServiceConnection
    static MongoDBContainer mongoDB = new MongoDBContainer("mongo:8.0").withEnv("GLIBC_TUNABLES", "glibc.pthread.rseq=1");

    @ServiceConnection
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:4-management");
}
