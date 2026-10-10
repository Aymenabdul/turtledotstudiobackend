-- =========================================================
-- V3__add_commercial_albums_and_guest_purchases.sql
-- Support for commercial candid albums and guest photo purchases
-- Compatible with MySQL 8 and H2 MySQL mode
-- =========================================================

-- Albums Table: Add commercial candid fields
ALTER TABLE albums ADD COLUMN is_commercial BOOLEAN DEFAULT FALSE;
ALTER TABLE albums ADD COLUMN price_per_photo DECIMAL(10,2) DEFAULT NULL;
ALTER TABLE albums ADD COLUMN allow_guest_purchases BOOLEAN DEFAULT TRUE;

-- Guest Purchases Table
CREATE TABLE IF NOT EXISTS guest_purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    album_id BIGINT NOT NULL,
    media_id BIGINT NOT NULL,
    qr_token VARCHAR(100) NOT NULL,
    guest_name VARCHAR(150),
    guest_phone VARCHAR(50),
    guest_email VARCHAR(150),
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'UPI',
    payment_reference VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guest_purchases_studio FOREIGN KEY (studio_id) REFERENCES studios(id) ON DELETE CASCADE,
    CONSTRAINT fk_guest_purchases_media FOREIGN KEY (media_id) REFERENCES media(id) ON DELETE CASCADE
);

CREATE INDEX idx_guest_purchases_token ON guest_purchases(qr_token);
CREATE INDEX idx_guest_purchases_media ON guest_purchases(media_id);
