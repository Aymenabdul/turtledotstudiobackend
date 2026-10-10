package com.turtledot.studio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("mysql")
class MySQLDatabaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testMySQLConnectionAndFlywayMigration() throws Exception {
        assertNotNull(dataSource, "DataSource should be initialized");
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            assertNotNull(conn, "Connection should be established to MySQL");

            // Check database product name
            String product = conn.getMetaData().getDatabaseProductName();
            System.out.println("Connected to: " + product + " version: " + conn.getMetaData().getDatabaseProductVersion());
            assertTrue(product.toLowerCase().contains("mysql"), "Should be connected to MySQL database");

            // Verify Flyway created tables in studio_crm
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'studios'")) {
                assertTrue(rs.next(), "Table 'studios' should exist in MySQL database");
            }
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'users'")) {
                assertTrue(rs.next(), "Table 'users' should exist in MySQL database");
            }
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'clients'")) {
                assertTrue(rs.next(), "Table 'clients' should exist in MySQL database");
            }
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'events'")) {
                assertTrue(rs.next(), "Table 'events' should exist in MySQL database");
            }
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'albums'")) {
                assertTrue(rs.next(), "Table 'albums' should exist in MySQL database");
            }
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'media'")) {
                assertTrue(rs.next(), "Table 'media' should exist in MySQL database");
            }
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'payments'")) {
                assertTrue(rs.next(), "Table 'payments' should exist in MySQL database");
            }
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'qr_tokens'")) {
                assertTrue(rs.next(), "Table 'qr_tokens' should exist in MySQL database");
            }
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'audit_logs'")) {
                assertTrue(rs.next(), "Table 'audit_logs' should exist in MySQL database");
            }

            // Verify SeedDataInitializer seeded default studio and demo users
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM users")) {
                assertTrue(rs.next());
                int count = rs.getInt(1);
                System.out.println("Seeded users count in MySQL: " + count);
                assertTrue(count >= 2, "Default admin and employee users should be seeded");
            }
        }
    }
}
