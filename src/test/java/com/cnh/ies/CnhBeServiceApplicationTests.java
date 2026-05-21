package com.cnh.ies;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires PostgreSQL, Redis, and full schema; passkey features are covered by focused unit tests.")
class CnhBeServiceApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally disabled in CI/local without infrastructure dependencies.
    }
}
