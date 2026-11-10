package ro.frit.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import ro.frit.notification.messages.BookingFact;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    // Stores the notification once per message id: a repeated delivery is ignored, not stored twice.
    public void notify(String messageId, BookingFact fact) {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .messageId(messageId)
                .bookingId(fact.bookingId())
                .customerEmail(fact.customerEmail())
                .text(NotificationTexts.textOf(fact))
                .notifiedAt(LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS))
                .build();
        try {
            notificationRepository.insert(notification);
        } catch (DuplicateKeyException alreadyHandled) {
            // Spring translates MongoDB's duplicate key error (E11000) on the unique messageId index into this.
            log.info("Message {} was already handled; the repeated delivery is ignored", messageId);
        }
    }

    public List<Notification> findAll(String customerEmail) {
        return customerEmail == null || customerEmail.isBlank()
                ? notificationRepository.findAllByOrderByNotifiedAtDesc()
                : notificationRepository.findByCustomerEmailIgnoreCaseOrderByNotifiedAtDesc(customerEmail);
    }
}
