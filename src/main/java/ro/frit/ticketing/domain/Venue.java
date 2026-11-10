package ro.frit.ticketing.domain;

// A value: two venues with the same name and city are the same venue.
// The compact constructor runs on every creation, so an invalid venue cannot exist.
public record Venue(String name, String city) {

    public Venue {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A venue needs a name");
        }
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("A venue needs a city");
        }
    }
}
