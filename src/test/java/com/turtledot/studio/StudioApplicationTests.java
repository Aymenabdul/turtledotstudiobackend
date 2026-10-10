package com.turtledot.studio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class StudioApplicationTests {

    @Test
    void contextLoads() {
        // Verifies complete Spring context, Flyway migration, JPA entities, and SeedDataInitializer
    }
}
