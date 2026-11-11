package ro.frit.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

// Where the services are, how many requests a client may send, and whether the services are private Cloud Run
// services. Only this changes between a local run, Docker Compose and Cloud Run.
@ConfigurationProperties("gateway")
public record GatewayProperties(URI ticketingUri, URI notificationsUri, RateLimit rateLimit, CloudRunAuth cloudRunAuth) {

    // At most `capacity` requests per `period` for each client, across all routes.
    public record RateLimit(long capacity, Duration period) {
    }

    // On Cloud Run the services accept only callers with a Google ID token. When enabled, the gateway gets one per
    // service from the metadata server every Cloud Run instance has. Off locally and in Compose: no such server.
    public record CloudRunAuth(boolean enabled, URI metadataUri) {
    }
}
