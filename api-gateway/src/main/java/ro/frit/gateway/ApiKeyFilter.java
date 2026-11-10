package ro.frit.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// Only callers that know the key get through: SkillForge's server sends it in X-Api-Key, never a browser.
// The key comes from the GATEWAY_API_KEY environment variable. Without it (local development) the check is off.
// Runs after CorrelationIdFilter, so even a 401 carries a correlation id.
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {

    private final byte[] expectedKey;

    public ApiKeyFilter(@Value("${gateway.api-key:}") String apiKey) {
        this.expectedKey = apiKey.isBlank() ? null : apiKey.getBytes(StandardCharsets.UTF_8);
        if (expectedKey == null) {
            log.warn("No gateway.api-key (GATEWAY_API_KEY) set: every caller gets through. Fine locally, never in the cloud.");
        }
    }

    // Health checks (Docker, Cloud Run) never carry a key.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return expectedKey == null || request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String sent = request.getHeader(Routes.API_KEY_HEADER);
        // MessageDigest.isEqual takes the same time whatever the input: a wrong key reveals nothing about the right one.
        if (sent != null && MessageDigest.isEqual(expectedKey, sent.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        log.info("Refused {} {}: {} API key", request.getMethod(), request.getRequestURI(), sent == null ? "no" : "wrong");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"type":"about:blank","title":"Unauthorized","status":401,"detail":"A valid X-Api-Key header is required.","instance":"%s"}"""
                .formatted(request.getRequestURI()));
    }
}
