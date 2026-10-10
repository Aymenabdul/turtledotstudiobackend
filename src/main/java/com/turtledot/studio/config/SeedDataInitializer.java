package com.turtledot.studio.config;

import com.turtledot.studio.studio.Studio;
import com.turtledot.studio.studio.StudioRepository;
import com.turtledot.studio.studio.StudioStatus;
import com.turtledot.studio.user.Role;
import com.turtledot.studio.user.User;
import com.turtledot.studio.user.UserRepository;
import com.turtledot.studio.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class SeedDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataInitializer.class);

    private final StudioRepository studioRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public SeedDataInitializer(StudioRepository studioRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               JdbcTemplate jdbcTemplate) {
        this.studioRepository = studioRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("Checking database seed status...");

        // 1. Ensure Default Studio exists
        Studio studio = studioRepository.findBySlug("auralens-studio").orElseGet(() -> {
            Studio s = new Studio();
            s.setName("Turtledot Studio");
            s.setSlug("auralens-studio");
            s.setLogoUrl("/turtledot_app_icon_color.png");
            s.setEmail("contact@turtledot.local");
            s.setPhone("+1 (555) 234-5678");
            s.setAddress("742 Evergreen Terrace, Creative District, NY 10001");
            s.setWebsite("https://turtledot.local");
            s.setCurrency("USD");
            s.setTimezone("America/New_York");
            s.setStatus(StudioStatus.ACTIVE);
            s.setMaxUploadSizeMb(10240);
            return studioRepository.save(s);
        });

        if ("AuraLens Creative Studio".equals(studio.getName()) || studio.getLogoUrl() == null || studio.getLogoUrl().isBlank()) {
            studio.setName("Turtledot Studio");
            studio.setLogoUrl("/turtledot_app_icon_color.png");
            studioRepository.save(studio);
        }

        if (studio.getGstin() == null || studio.getGstin().isBlank()) {
            studio.setGstin("27AAPFU0939F1ZV");
            studio.setPanNumber("AAPFU0939F");
            studio.setStateName("Maharashtra");
            studio.setStateCode("27");
            studio.setGstRatePercent(new java.math.BigDecimal("18.00"));
            studioRepository.save(studio);
        }

        // 2. Ensure Admin User exists
        if (!userRepository.existsByEmail("admin@studio.local")) {
            User admin = new User(
                    studio.getId(),
                    "admin@studio.local",
                    passwordEncoder.encode("Admin@123456"),
                    "Studio Director",
                    "+1 (555) 000-0001",
                    Role.ADMIN,
                    UserStatus.ACTIVE
            );
            userRepository.save(admin);
            log.info("Admin user seeded: admin@studio.local / Admin@123456");
        }

        // 3. Ensure Employee User exists
        User employeeUser = userRepository.findByEmail("employee@studio.local").orElseGet(() -> {
            User emp = new User(
                    studio.getId(),
                    "employee@studio.local",
                    passwordEncoder.encode("Employee@123456"),
                    "Alex Rivera",
                    "+1 (555) 000-0002",
                    Role.EMPLOYEE,
                    UserStatus.ACTIVE
            );
            User saved = userRepository.save(emp);
            log.info("Employee user seeded: employee@studio.local / Employee@123456");
            return saved;
        });

        // 4. Ensure Employee Profile Record exists
        Integer empCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE user_id = ?",
                Integer.class,
                employeeUser.getId()
        );
        Long employeeId;
        if (empCount == null || empCount == 0) {
            jdbcTemplate.update(
                    "INSERT INTO employees (studio_id, user_id, employee_code, designation, department, notes) VALUES (?, ?, ?, ?, ?, ?)",
                    studio.getId(), employeeUser.getId(), "EMP-001", "Senior Lead Photographer", "Wedding & Events", "Specializes in Candid & Portrait Photography"
            );
            employeeId = jdbcTemplate.queryForObject("SELECT id FROM employees WHERE user_id = ?", Long.class, employeeUser.getId());

            // Seed initial employee permissions
            String[] perms = {
                    "CLIENT_VIEW", "PHOTO_UPLOAD", "PHOTO_DELETE",
                    "VIDEO_UPLOAD", "ALBUM_CREATE", "ALBUM_EDIT",
                    "MEDIA_DOWNLOAD", "CLIENT_EDIT"
            };
            for (String p : perms) {
                jdbcTemplate.update(
                        "INSERT INTO employee_permissions (studio_id, employee_id, permission) VALUES (?, ?, ?)",
                        studio.getId(), employeeId, p
                );
            }
        } else {
            employeeId = jdbcTemplate.queryForObject("SELECT id FROM employees WHERE user_id = ?", Long.class, employeeUser.getId());
        }

        // 5. Ensure Demo Clients exist
        Integer clientCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clients WHERE studio_id = ?",
                Integer.class,
                studio.getId()
        );

        if (clientCount == null || clientCount == 0) {
            log.info("Seeding realistic demo clients, events, albums, and QR gallery tokens...");

            // Client 1: Rajesh & Priya
            jdbcTemplate.update(
                    "INSERT INTO clients (studio_id, client_code, name, email, phone, city, state, country, status, notes, total_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    studio.getId(), "CL-2026-000001", "Rajesh Kumar & Priya Sharma", "rajesh.priya@example.com", "+1 (555) 345-6789", "Manhattan", "NY", "USA", "ACTIVE", "Destination Wedding package. Premium cinematography and drone coverage included.", 6500.00
            );
            Long client1Id = jdbcTemplate.queryForObject("SELECT id FROM clients WHERE client_code = 'CL-2026-000001'", Long.class);

            // Client 2: Michael & Sarah
            jdbcTemplate.update(
                    "INSERT INTO clients (studio_id, client_code, name, email, phone, city, state, country, status, notes, total_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    studio.getId(), "CL-2026-000002", "Michael & Sarah Jenkins", "michael.jenkins@example.com", "+1 (555) 456-7890", "Brooklyn", "NY", "USA", "ACTIVE", "Classic Garden Wedding & Pre-wedding portrait collection.", 4800.00
            );
            Long client2Id = jdbcTemplate.queryForObject("SELECT id FROM clients WHERE client_code = 'CL-2026-000002'", Long.class);

            // Events for Client 1
            jdbcTemplate.update(
                    "INSERT INTO events (studio_id, client_id, event_code, event_name, event_type, event_date, location, description, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    studio.getId(), client1Id, "EV-2026-0001", "Grand Wedding Ceremony & Reception", "Wedding", java.sql.Date.valueOf(LocalDate.now().minusDays(10)), "The Plaza Hotel, Grand Ballroom, NYC", "3-day royal wedding ceremony with reception.", "DELIVERED"
            );
            Long event1Id = jdbcTemplate.queryForObject("SELECT id FROM events WHERE event_code = 'EV-2026-0001'", Long.class);

            jdbcTemplate.update(
                    "INSERT INTO events (studio_id, client_id, event_code, event_name, event_type, event_date, location, description, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    studio.getId(), client1Id, "EV-2026-0002", "Pre-Wedding Sunset Shoot", "Pre-Wedding", java.sql.Date.valueOf(LocalDate.now().minusDays(30)), "Central Park Conservatory & Brooklyn Bridge", "Golden hour romance session.", "DELIVERED"
            );
            Long event2Id = jdbcTemplate.queryForObject("SELECT id FROM events WHERE event_code = 'EV-2026-0002'", Long.class);

            // Events for Client 2
            jdbcTemplate.update(
                    "INSERT INTO events (studio_id, client_id, event_code, event_name, event_type, event_date, location, description, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    studio.getId(), client2Id, "EV-2026-0003", "Botanical Garden Wedding", "Wedding", java.sql.Date.valueOf(LocalDate.now().plusDays(15)), "Brooklyn Botanic Garden, NY", "Outdoor ceremony with 150 guests.", "UPCOMING"
            );
            Long event3Id = jdbcTemplate.queryForObject("SELECT id FROM events WHERE event_code = 'EV-2026-0003'", Long.class);

            // Albums for Event 1
            jdbcTemplate.update(
                    "INSERT INTO albums (studio_id, client_id, event_id, name, description, status) VALUES (?, ?, ?, ?, ?, ?)",
                    studio.getId(), client1Id, event1Id, "Ceremony Highlights", "Vows, rings exchange, and rituals", "ACTIVE"
            );
            Long album1Id = jdbcTemplate.queryForObject("SELECT id FROM albums WHERE event_id = ? AND name = 'Ceremony Highlights'", Long.class, event1Id);

            jdbcTemplate.update(
                    "INSERT INTO albums (studio_id, client_id, event_id, name, description, status) VALUES (?, ?, ?, ?, ?, ?)",
                    studio.getId(), client1Id, event1Id, "Reception & Dance Party", "First dance, toasts, and cake cutting", "ACTIVE"
            );
            Long album2Id = jdbcTemplate.queryForObject("SELECT id FROM albums WHERE event_id = ? AND name = 'Reception & Dance Party'", Long.class, event1Id);

            jdbcTemplate.update(
                    "INSERT INTO albums (studio_id, client_id, event_id, name, description, status) VALUES (?, ?, ?, ?, ?, ?)",
                    studio.getId(), client1Id, event1Id, "Couple & Bridal Portraits", "Stunning editorial portraits of Rajesh & Priya", "ACTIVE"
            );
            Long album3Id = jdbcTemplate.queryForObject("SELECT id FROM albums WHERE event_id = ? AND name = 'Couple & Bridal Portraits'", Long.class, event1Id);

            // Seed Sample Media Records for immediate visual wow factor
            String[] samplePhotos = {
                    "https://images.unsplash.com/photo-1519741497674-611481863552?q=80&w=1200&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1511285560929-80b456fea0bc?q=80&w=1200&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1465495976277-4387d4b0b4c6?q=80&w=1200&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1583939003579-730e3918a45a?q=80&w=1200&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1606800052052-a08af7148866?q=80&w=1200&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1532712938310-34cb3982ef74?q=80&w=1200&auto=format&fit=crop"
            };

            for (int i = 0; i < samplePhotos.length; i++) {
                String photoUrl = samplePhotos[i];
                String storedName = UUID.randomUUID().toString() + ".jpg";
                jdbcTemplate.update(
                        "INSERT INTO media (studio_id, client_id, event_id, album_id, media_type, file_name, stored_file_name, file_path, file_url, thumbnail_url, preview_url, mime_type, file_size, width, height, is_favorite, uploaded_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        studio.getId(), client1Id, event1Id, (i < 2 ? album1Id : (i < 4 ? album2Id : album3Id)),
                        "PHOTO", "Wedding_Capture_" + (i + 1) + ".jpg", storedName,
                        "./storage/sample/" + storedName, photoUrl, photoUrl, photoUrl,
                        "image/jpeg", 4200000L, 3840, 2160, (i % 2 == 0), employeeUser.getId()
                );
            }

            // Payments
            jdbcTemplate.update(
                    "INSERT INTO payments (studio_id, client_id, event_id, amount, payment_method, payment_reference, payment_date, notes, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    studio.getId(), client1Id, event1Id, 4500.00, "BANK_TRANSFER", "WIRE-2026-9812", java.sql.Date.valueOf(LocalDate.now().minusDays(15)), "Initial advance & booking confirmation deposit.", employeeUser.getId()
            );

            jdbcTemplate.update(
                    "INSERT INTO payments (studio_id, client_id, event_id, amount, payment_method, payment_reference, payment_date, notes, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    studio.getId(), client2Id, event3Id, 2000.00, "CARD", "TXN-CARD-8831", java.sql.Date.valueOf(LocalDate.now().minusDays(5)), "Booking token advance for botanical garden event.", employeeUser.getId()
            );

            // Assign Employee to Client 1 & Client 2
            jdbcTemplate.update(
                    "INSERT INTO employee_client_access (studio_id, employee_id, client_id, event_id) VALUES (?, ?, ?, ?)",
                    studio.getId(), employeeId, client1Id, null
            );
            jdbcTemplate.update(
                    "INSERT INTO employee_client_access (studio_id, employee_id, client_id, event_id) VALUES (?, ?, ?, ?)",
                    studio.getId(), employeeId, client2Id, null
            );

            // Seed Dedicated Demo QR Gallery Token for Rajesh & Priya
            String demoQrToken = "auralens-demo-gallery-2026";
            jdbcTemplate.update(
                    "INSERT INTO qr_tokens (studio_id, client_id, event_id, token_value, token_hash, status, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    studio.getId(), client1Id, event1Id, demoQrToken, demoQrToken, "ACTIVE", java.sql.Timestamp.valueOf(LocalDate.now().plusMonths(6).atStartOfDay())
            );

            log.info("Demo QR token seeded: {}", demoQrToken);
        }

        log.info("Seed data initialization complete.");
    }
}
