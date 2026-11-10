package ro.frit.ticketing.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

// In the tests, "now" is always 30 November 2026, 12:00, so "has the event started?" never depends on the real date.
@TestConfiguration
public class FixedClockConfiguration {

    public static final Instant NOW = Instant.parse("2026-11-30T12:00:00Z");
    public static final LocalDateTime TODAY_AT_NOON = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
