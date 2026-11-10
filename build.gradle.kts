// Each service has its own build file (ticketing-service/, notification-service/).
// The root only resolves the plugin versions once, for all of them.
plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}
