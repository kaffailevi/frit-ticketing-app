package ro.frit.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

// The routes, in the Java DSL of Spring Cloud Gateway Server WebMVC (servlet-based, like the services):
// a path decides the service; the request goes on unchanged (method, path, query, headers, body).
@Configuration
public class Routes {

    @Bean
    RouterFunction<ServerResponse> ticketingRoute(GatewayProperties properties) {
        return route("ticketing")
                .route(path("/events", "/events/**"), http())
                .before(uri(properties.ticketingUri()))
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> notificationsRoute(GatewayProperties properties) {
        return route("notifications")
                .route(path("/notifications", "/notifications/**"), http())
                .before(uri(properties.notificationsUri()))
                .build();
    }
}
