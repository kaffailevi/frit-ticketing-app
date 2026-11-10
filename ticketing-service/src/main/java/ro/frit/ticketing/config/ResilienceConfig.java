package ro.frit.ticketing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

// Spring Framework 7: turns on @Retryable and @ConcurrencyLimit on bean methods (no extra library needed).
@Configuration
@EnableResilientMethods
public class ResilienceConfig {
}
