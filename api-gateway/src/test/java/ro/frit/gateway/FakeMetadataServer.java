package ro.frit.gateway;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

// Stands in for the metadata server of a Cloud Run instance. Like the real one, it refuses requests without
// "Metadata-Flavor: Google". For an identity token it answers "token-<n>-for-<audience>", n counting the tokens
// handed out, so a test sees which service a token is for and whether it is a new one.
final class FakeMetadataServer {

    private final HttpServer server;
    private final AtomicInteger tokensHandedOut = new AtomicInteger();

    FakeMetadataServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/computeMetadata/v1/instance/service-accounts/default/identity", exchange -> {
            boolean fromInside = "Google".equals(exchange.getRequestHeaders().getFirst("Metadata-Flavor"));
            String query = exchange.getRequestURI().getRawQuery();
            String audience = query == null ? "" : URLDecoder.decode(query.replaceFirst("^audience=", ""), StandardCharsets.UTF_8);
            byte[] body = fromInside
                    ? ("token-" + tokensHandedOut.incrementAndGet() + "-for-" + audience).getBytes(StandardCharsets.UTF_8)
                    : "Missing Metadata-Flavor:Google header.".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(fromInside ? 200 : 403, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    String uri() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    int tokensHandedOut() {
        return tokensHandedOut.get();
    }

    void stop() {
        server.stop(0);
    }
}
