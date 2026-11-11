package ro.frit.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// The gateway's badge for private Cloud Run services. A private service answers 403 to anyone without a Google ID
// token whose audience is the service's own URL. The gateway runs as a service account allowed to call both
// services (roles/run.invoker); the metadata server of its instance hands out tokens for that account.
@Slf4j
@Component
public class IdentityTokens {

    // A token is valid for an hour; take a new one well before that.
    static final Duration REUSE_FOR = Duration.ofMinutes(50);
    private static final String IDENTITY_PATH = "/computeMetadata/v1/instance/service-accounts/default/identity";

    private final boolean enabled;
    private final RestClient metadataServer;
    private final Clock clock;
    private final Map<URI, Token> tokens = new ConcurrentHashMap<>();

    @Autowired
    public IdentityTokens(GatewayProperties properties) {
        this(properties.cloudRunAuth(), Clock.systemUTC());
    }

    IdentityTokens(GatewayProperties.CloudRunAuth settings, Clock clock) {
        this.enabled = settings.enabled();
        this.clock = clock;
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(2));
        timeouts.setReadTimeout(Duration.ofSeconds(2));
        this.metadataServer = RestClient.builder()
                .baseUrl(settings.metadataUri().toString())
                .defaultHeader("Metadata-Flavor", "Google") // the metadata server answers only requests that say this
                .requestFactory(timeouts)
                .build();
    }

    // A route filter: the request leaves with "Authorization: Bearer <token for this service>". Whatever
    // Authorization header the client sent is replaced, never passed on. Off: the request goes on unchanged.
    public HandlerFilterFunction<ServerResponse, ServerResponse> authorizeFor(URI service) {
        if (!enabled) {
            return (request, next) -> next.handle(request);
        }
        return (request, next) -> {
            String token;
            try {
                token = tokenFor(service);
            } catch (RestClientException e) {
                log.error("No identity token for {}: {}", service, e.getMessage());
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                        "The gateway could not identify itself to the service. Try again in a moment.");
                return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body(problem);
            }
            return next.handle(ServerRequest.from(request).headers(headers -> headers.setBearerAuth(token)).build());
        };
    }

    String tokenFor(URI audience) {
        Instant now = clock.instant();
        return tokens.compute(audience, (uri, token) ->
                token != null && now.isBefore(token.fetchedAt().plus(REUSE_FOR)) ? token : new Token(fetch(uri), now)
        ).value();
    }

    private String fetch(URI audience) {
        log.info("Fetching an identity token for {}", audience);
        return metadataServer.get()
                .uri(uri -> uri.path(IDENTITY_PATH).queryParam("audience", audience).build())
                .retrieve()
                .body(String.class);
    }

    private record Token(String value, Instant fetchedAt) {
    }
}
