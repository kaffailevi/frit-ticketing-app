// Java 25: a compact source file. No class, no "public static", no String[] args;
// java.base is imported as a module, so List, Map and friends need no import.
// Run it without compiling: java Hello.java

void main() {
    var cities = List.of("Cluj-Napoca", "Brasov", "Iasi");
    IO.println("Hello from Java " + Runtime.version().feature() + ": events in " + cities);
}
