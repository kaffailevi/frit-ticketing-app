package ro.frit.notification;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findAllByOrderByNotifiedAtDesc();

    List<Notification> findByCustomerEmailIgnoreCaseOrderByNotifiedAtDesc(String customerEmail);
}
