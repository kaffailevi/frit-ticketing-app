package frit;

public class JavaVersionTest {

    @Test
    void testJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        System.out.println("Java version: " + javaVersion);
        assertTrue(javaVersion.startsWith("17"), "Java version should be 17");
    }

}
