package ro.frit.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

// Where the services are. Only this changes between a local run, Docker Compose and Cloud Run.
@ConfigurationProperties("gateway")
public record GatewayProperties(URI ticketingUri, URI notificationsUri) {
}
