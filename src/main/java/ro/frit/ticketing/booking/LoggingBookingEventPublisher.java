package ro.frit.ticketing.booking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// The first adapter of the BookingEventPublisher port: it only writes the facts to the log.
@Slf4j
@Component
public class LoggingBookingEventPublisher implements BookingEventPublisher {

    @Override
    public void publish(BookingConfirmed confirmed) {
        log.info("Booking confirmed: {}", confirmed);
    }

    @Override
    public void publish(BookingCancelled cancelled) {
        log.info("Booking cancelled: {}", cancelled);
    }

    @Override
    public void publish(BookedEventChanged changed) {
        log.info("Booked event changed: {}", changed);
    }
}
