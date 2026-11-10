package ro.frit.ticketing.domain;

// What a customer asks for. Invalid requests are refused at creation, before they reach the TicketOffice.
public record BookingRequest(String eventId, String customerEmail, int seats) {

    public BookingRequest {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("A booking needs an event id");
        }
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("A booking needs the customer email");
        }
        if (seats <= 0) {
            throw new IllegalArgumentException("A booking needs at least one seat");
        }
    }
}
