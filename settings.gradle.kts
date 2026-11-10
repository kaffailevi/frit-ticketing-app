rootProject.name = "ticketing-app"

// One repository, one Gradle build, several services; each is its own Spring Boot application.
include("ticketing-service", "notification-service")
