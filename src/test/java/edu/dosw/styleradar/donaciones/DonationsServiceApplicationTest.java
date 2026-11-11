package edu.dosw.styleradar.donaciones;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DonationsServiceApplicationTest {

    @Test
    @DisplayName("Debe instanciar la clase principal de arranque de Spring Boot")
    void contextLoads() {
        DonationsServiceApplication application = new DonationsServiceApplication();
        assertNotNull(application);
    }
}
