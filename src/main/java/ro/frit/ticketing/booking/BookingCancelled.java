package ro.frit.ticketing.booking;

public record BookingCancelled(String bookingId, String eventId, String customerEmail, int seats, Reason reason) {

    public enum Reason { BY_CUSTOMER, EVENT_CANCELLED }

    static BookingCancelled of(Booking booking, Reason reason) {
        return new BookingCancelled(booking.getId(), booking.getEventId(), booking.getCustomerEmail(), booking.getSeats(), reason);
    }
}
