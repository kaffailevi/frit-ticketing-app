package ro.frit.ticketing.shared.error;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

// Turns the application's exceptions into ProblemDetail responses in one place, so the controllers only
// describe the happy path. Spring's own errors (invalid body, wrong content type, failed validation) are
// still answered by Spring Boot, because spring.mvc.problemdetails.enabled is true.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail conflict(ConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(CONFLICT, exception.getMessage());
        exception.properties().forEach(problem::setProperty);
        return problem;
    }
}
