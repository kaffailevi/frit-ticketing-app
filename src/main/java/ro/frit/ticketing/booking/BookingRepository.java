package ro.frit.ticketing.booking;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

// CRUD from Spring Data; the reports come from the BookingReportRepository fragment.
public interface BookingRepository extends MongoRepository<Booking, String>, BookingReportRepository {

    // Derived queries: the method names say what MongoDB is asked.
    Optional<Booking> findByIdAndEventId(String id, String eventId);

    List<Booking> findByEventIdOrderByBookedAt(String eventId);
}
