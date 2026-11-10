package ro.frit.ticketing.config;

import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.mapping.event.ValidatingEntityCallback;

// Checks the Bean Validation constraints of a document (@NotBlank, @Positive, ...) before Spring Data saves it.
// An invalid document throws ConstraintViolationException and nothing is written.
@Configuration
public class MongoConfig {

    @Bean
    public ValidatingEntityCallback validatingEntityCallback(Validator validator) {
        return new ValidatingEntityCallback(validator);
    }
}
