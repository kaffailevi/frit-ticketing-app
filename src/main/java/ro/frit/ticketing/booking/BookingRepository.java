package ro.frit.ticketing.booking;

import org.springframework.data.mongodb.repository.MongoRepository;

// CRUD from Spring Data; the reports come from the BookingReportRepository fragment.
public interface BookingRepository extends MongoRepository<Booking, String>, BookingReportRepository {
}
