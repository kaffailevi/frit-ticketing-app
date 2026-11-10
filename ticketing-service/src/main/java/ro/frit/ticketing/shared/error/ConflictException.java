package ro.frit.ticketing.shared.error;

import java.util.Map;

// Base class for "the request is valid, but the current state says no" (sold out, already started).
// GlobalExceptionHandler answers it with 409 and copies the properties into the ProblemDetail.
public abstract class ConflictException extends RuntimeException {

    private final Map<String, Object> properties;

    protected ConflictException(String message, Map<String, Object> properties) {
        super(message);
        this.properties = Map.copyOf(properties);
    }

    public Map<String, Object> properties() {
        return properties;
    }
}
