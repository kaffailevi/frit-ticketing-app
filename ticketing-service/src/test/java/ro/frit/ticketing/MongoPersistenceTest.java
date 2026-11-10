package ro.frit.ticketing;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Import;
import ro.frit.ticketing.config.FixedClockConfiguration;
import ro.frit.ticketing.config.MongoConfig;
import ro.frit.ticketing.config.ResilienceConfig;
import ro.frit.ticketing.config.TestcontainersConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// A persistence test: only the MongoDB part of Spring (@DataMongoTest), a MongoDB in Docker, the same validation
// before saving and the same retries as the application has, and a fixed clock. Add the services under test with @Import.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataMongoTest
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
@Import({TestcontainersConfiguration.class, MongoConfig.class, ResilienceConfig.class, FixedClockConfiguration.class})
public @interface MongoPersistenceTest {
}
