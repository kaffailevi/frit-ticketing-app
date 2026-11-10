package ro.frit.ticketing.booking;

// A port: what BookingService needs said to the outside world, in its own words, with no technology in it.
// Adapters implement it: today LoggingBookingEventPublisher, from session 6 one that sends RabbitMQ messages,
// in tests a mock. BookingService never changes when the adapter does.
public interface BookingEventPublisher {

    void publish(BookingConfirmed confirmed);

    void publish(BookingCancelled cancelled);

    void publish(BookedEventChanged changed);
}
