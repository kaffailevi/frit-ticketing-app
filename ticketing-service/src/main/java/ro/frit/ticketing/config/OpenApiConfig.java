package ro.frit.ticketing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// springdoc reads the controllers and publishes an OpenAPI description:
//   /v3/api-docs        the description as JSON (clients can be generated from it)
//   /swagger-ui.html    a page to read it and try every endpoint
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketingApi() {
        return new OpenAPI().info(new Info()
                .title("Ticketing API")
                .version("1")
                .description("Learning events and bookings. Every error is a ProblemDetail (application/problem+json)."));
    }
}
