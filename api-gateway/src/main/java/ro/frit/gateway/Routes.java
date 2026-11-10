package ro.frit.gateway;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.caffeine.Bucket4jCaffeine;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.Bucket4jFilterFunctions.rateLimit;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

// The routes, in the Java DSL of Spring Cloud Gateway Server WebMVC (servlet-based, like the services):
// a path decides the service; the request goes on unchanged (method, path, query, headers, body).
@Configuration
public class Routes {

    public static final String API_KEY_HEADER = "X-Api-Key";

    // Token buckets in memory, one per client key: each request takes a token, tokens come back over the period.
    // In memory means per gateway instance: two instances would allow twice the rate.
    @Bean
    AsyncProxyManager<String> rateLimitBuckets() {
        return Bucket4jCaffeine.<String>builderFor(Caffeine.newBuilder().maximumSize(100_000)).build().asAsync();
    }

    @Bean
    RouterFunction<ServerResponse> ticketingRoute(GatewayProperties properties) {
        return route("ticketing")
                .route(path("/events", "/events/**"), http())
                .before(uri(properties.ticketingUri()))
                .filter(perClientRateLimit(properties.rateLimit()))
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> notificationsRoute(GatewayProperties properties) {
        return route("notifications")
                .route(path("/notifications", "/notifications/**"), http())
                .before(uri(properties.notificationsUri()))
                .filter(perClientRateLimit(properties.rateLimit()))
                .build();
    }

    // Over the limit: 429 Too Many Requests, and Retry-After says when a token is back (period / capacity, in seconds).
    private static HandlerFilterFunction<ServerResponse, ServerResponse> perClientRateLimit(GatewayProperties.RateLimit limit) {
        HandlerFilterFunction<ServerResponse, ServerResponse> rateLimit = rateLimit(config -> config
                .setCapacity(limit.capacity())
                .setPeriod(limit.period())
                .setKeyResolver(Routes::clientKey));
        String retryAfter = String.valueOf(Math.max(1, Math.ceilDiv(limit.period().toSeconds(), limit.capacity())));
        return (request, next) -> {
            ServerResponse response = rateLimit.filter(request, next);
            if (!response.statusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
                return response;
            }
            return ServerResponse.from(response).header(HttpHeaders.RETRY_AFTER, retryAfter).build(); // the 429 is read-only
        };
    }

    // Who "one client" is. Behind Cloud Run's front end the connection always comes from Google's proxy, so the
    // address alone would put every client in one bucket. In order: the API key (one per calling application),
    // the first address in X-Forwarded-For (the original client), the connection's address.
    static String clientKey(ServerRequest request) {
        String apiKey = request.headers().firstHeader(API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank()) {
            return "key:" + Integer.toHexString(apiKey.hashCode()); // never keep the key itself in memory as a map key
        }
        String forwardedFor = request.headers().firstHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].strip();
        }
        return "ip:" + request.remoteAddress().map(address -> address.getAddress().getHostAddress()).orElse("unknown");
    }
}
