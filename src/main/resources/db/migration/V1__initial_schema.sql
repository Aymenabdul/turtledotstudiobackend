-- =========================================================
-- V1__initial_schema.sql
-- Photography Studio CRM and Client Photo Delivery Platform
-- Compatible with MySQL 8 and H2 MySQL mode
-- =========================================================

-- Studios Table
CREATE TABLE studios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    logo_url VARCHAR(500),
    phone VARCHAR(50),
    email VARCHAR(150),
    address VARCHAR(500),
    website VARCHAR(255),
    currency VARCHAR(10) DEFAULT 'USD',
    timezone VARCHAR(50) DEFAULT 'UTC',
    max_upload_size_mb INT DEFAULT 500,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Users Table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL, -- ADMIN, EMPLOYEE, CLIENT
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, DISABLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE
);

CREATE INDEX idx_users_studio_role ON users(studio_id, role);
CREATE INDEX idx_users_email ON users(email);

-- Employees Table
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL UNIQUE,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    designation VARCHAR(100),
    department VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employees_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_employees_studio ON employees(studio_id);

-- Clients Table
CREATE TABLE clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    user_id BIGINT,
    client_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(50),
    alternate_phone VARCHAR(50),
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    profile_image_url VARCHAR(500),
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, ARCHIVED, DELETED
    notes TEXT,
    total_amount DECIMAL(12,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_clients_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_clients_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_clients_studio_status ON clients(studio_id, status);
CREATE INDEX idx_clients_code ON clients(client_code);

-- Events Table
CREATE TABLE events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    event_code VARCHAR(50) NOT NULL UNIQUE,
    event_name VARCHAR(200) NOT NULL,
    event_type VARCHAR(100) NOT NULL, -- Wedding, Reception, Engagement, Birthday, etc.
    event_date DATE NOT NULL,
    location VARCHAR(300),
    description TEXT,
    status VARCHAR(50) DEFAULT 'UPCOMING', -- UPCOMING, IN_PROGRESS, COMPLETED, DELIVERED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_events_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_events_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

CREATE INDEX idx_events_studio_client ON events(studio_id, client_id);
CREATE INDEX idx_events_date ON events(event_date);

-- Albums Table
CREATE TABLE albums (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    cover_image_url VARCHAR(500),
    folder_path VARCHAR(500),
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, HIDDEN, ARCHIVED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_albums_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_albums_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_albums_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

CREATE INDEX idx_albums_event ON albums(event_id);

-- Media Table
CREATE TABLE media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    album_id BIGINT NOT NULL,
    media_type VARCHAR(20) NOT NULL, -- PHOTO, VIDEO
    file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL UNIQUE,
    file_path VARCHAR(600) NOT NULL,
    file_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    preview_url VARCHAR(500),
    mime_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    width INT,
    height INT,
    duration BIGINT, -- in milliseconds or seconds
    is_favorite BOOLEAN DEFAULT FALSE,
    uploaded_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_media_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_media_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_media_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_media_album FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE
);

CREATE INDEX idx_media_album ON media(album_id);
CREATE INDEX idx_media_client_type ON media(client_id, media_type);

-- Payments Table
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    event_id BIGINT,
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL, -- CASH, UPI, BANK_TRANSFER, CARD, OTHER
    payment_reference VARCHAR(150),
    payment_date DATE NOT NULL,
    notes TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE SET NULL
);

CREATE INDEX idx_payments_client ON payments(client_id);
CREATE INDEX idx_payments_date ON payments(payment_date);

-- Employee Client Access (Explicit Assignments)
CREATE TABLE employee_client_access (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    event_id BIGINT, -- NULL means all events for that client
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    CONSTRAINT fk_access_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_access_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_access_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_access_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

CREATE INDEX idx_access_employee_client ON employee_client_access(employee_id, client_id);

-- Employee Granular Permissions
CREATE TABLE employee_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    permission VARCHAR(100) NOT NULL, -- CLIENT_VIEW, PHOTO_UPLOAD, PHOTO_DELETE, VIDEO_UPLOAD, VIDEO_DELETE, ALBUM_CREATE, ALBUM_EDIT, ALBUM_DELETE, MEDIA_DOWNLOAD, CLIENT_EDIT
    CONSTRAINT fk_perm_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_perm_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT uq_employee_permission UNIQUE (employee_id, permission)
);

CREATE INDEX idx_perm_employee ON employee_permissions(employee_id);

-- QR Tokens Table
CREATE TABLE qr_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    event_id BIGINT,
    token_value VARCHAR(100) NOT NULL UNIQUE,
    token_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, DISABLED, EXPIRED
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP NULL,
    CONSTRAINT fk_qr_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_qr_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    CONSTRAINT fk_qr_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE SET NULL
);

CREATE INDEX idx_qr_token ON qr_tokens(token_value);

-- Refresh Tokens Table
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);

-- Audit Logs Table
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    description TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_studio_action ON audit_logs(studio_id, action);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- Studio Settings Table
CREATE TABLE studio_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_settings_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT uq_studio_setting UNIQUE (studio_id, setting_key)
);
