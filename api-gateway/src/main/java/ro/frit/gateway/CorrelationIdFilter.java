package ro.frit.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

// Gives every request one id: the client's X-Correlation-Id, or a new one. The id goes on to the service with the
// forwarded request, back to the client in the response, and into every log line (the MDC; see logging.pattern).
// The services log it too, and ticketing puts it on its RabbitMQ messages: one id follows a booking everywhere.
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(withHeader(request, correlationId), response);
            log.info("{} {} -> {}", request.getMethod(), request.getRequestURI(), response.getStatus());
        } finally {
            MDC.remove(MDC_KEY); // threads are reused: the next request must not inherit this id
        }
    }

    // The gateway forwards the request's own headers, so a newly made id is added to the request itself.
    private static HttpServletRequest withHeader(HttpServletRequest request, String correlationId) {
        if (correlationId.equals(request.getHeader(HEADER))) {
            return request;
        }
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                return HEADER.equalsIgnoreCase(name) ? correlationId : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                return HEADER.equalsIgnoreCase(name) ? Collections.enumeration(List.of(correlationId)) : super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                names.add(HEADER);
                return Collections.enumeration(names);
            }
        };
    }
}
