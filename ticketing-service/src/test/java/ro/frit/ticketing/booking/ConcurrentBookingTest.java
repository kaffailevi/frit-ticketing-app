package ro.frit.ticketing.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ro.frit.ticketing.MongoPersistenceTest;
import ro.frit.ticketing.booking.dto.CreateBookingRequest;
import ro.frit.ticketing.event.EventRepository;
import ro.frit.ticketing.event.EventService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ro.frit.ticketing.config.FixedClockConfiguration.TODAY_AT_NOON;
import static ro.frit.ticketing.event.EventFixtures.event;

// 20 one-seat bookings released at the same moment on an event with 10 seats never overbook it.
// The proof that it tests something: remove @Version from Event and it fails (more seats booked than exist).
@MongoPersistenceTest
@Import({BookingService.class, EventService.class})
class ConcurrentBookingTest {

    private static final int CAPACITY = 10;
    private static final int CUSTOMERS = 20;

    @MockitoBean // messaging is not what these tests are about; RabbitBookingEventPublisherTest covers it
    private BookingEventPublisher bookingEvents;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void oneEmptyEvent() {
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        eventRepository.save(event("popular", "Cluj-Napoca", TODAY_AT_NOON.plusDays(7), CAPACITY, 0));
    }

    @Test
    void bookingsAtTheSameMomentNeverExceedTheCapacity() throws Exception {
        CountDownLatch everyoneReady = new CountDownLatch(CUSTOMERS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> attempts = new ArrayList<>();

        try (ExecutorService customers = Executors.newFixedThreadPool(CUSTOMERS)) {
            for (int i = 0; i < CUSTOMERS; i++) {
                CreateBookingRequest request = new CreateBookingRequest("fan" + i + "@example.com", 1);
                attempts.add(customers.submit(() -> {
                    everyoneReady.countDown();
                    go.await(); // all threads wait here, then book at the same moment
                    return bookingService.book("popular", request);
                }));
            }
            try {
                assertTrue(everyoneReady.await(10, TimeUnit.SECONDS), "all customers at the start line");
            } finally {
                go.countDown(); // always open the gate, so a failed wait cannot hang the test
            }
        }

        int confirmed = 0;
        for (Future<?> attempt : attempts) {
            try {
                attempt.get();
                confirmed++;
            } catch (ExecutionException soldOutOrLostTheRace) {
                // not confirmed: sold out, or still losing after the retries (a 409 for that customer)
            }
        }

        int seatsOnTheEvent = eventRepository.findById("popular").orElseThrow().getBookedSeats();
        int seatsInBookings = bookingRepository.findAll().stream().mapToInt(Booking::getSeats).sum();
        assertTrue(seatsInBookings <= CAPACITY, seatsInBookings + " seats booked on an event with " + CAPACITY);
        assertEquals(seatsInBookings, seatsOnTheEvent, "every booked seat is counted on the event");
        assertEquals(confirmed, seatsInBookings, "every confirmed booking is stored");
        assertTrue(confirmed >= 1, "at least one booking is confirmed");
    }
}
