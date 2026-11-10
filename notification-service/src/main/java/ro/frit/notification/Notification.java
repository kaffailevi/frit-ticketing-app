package ro.frit.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// One message to one customer, stored in this service's own database.
@Getter
@ToString
@Builder
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Document(collection = "notifications")
public class Notification {

    @Id
    private final String id;

    // Idempotency: RabbitMQ delivers "at least once", so the same message can arrive twice.
    // The unique index makes MongoDB refuse the second copy.
    @Indexed(unique = true)
    private final String messageId;

    private final String bookingId;

    @Indexed
    private final String customerEmail;

    private final String text;

    private final LocalDateTime notifiedAt;
}
