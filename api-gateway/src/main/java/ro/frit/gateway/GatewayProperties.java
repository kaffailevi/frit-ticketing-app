package ro.frit.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

// Where the services are, and how many requests a client may send. Only this changes between a local run,
// Docker Compose and Cloud Run.
@ConfigurationProperties("gateway")
public record GatewayProperties(URI ticketingUri, URI notificationsUri, RateLimit rateLimit) {

    // At most `capacity` requests per `period` for each client, across all routes.
    public record RateLimit(long capacity, Duration period) {
    }
}
