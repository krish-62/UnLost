USE unlost;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE items
    MODIFY status ENUM('Open', 'Returned', 'Resolved') NOT NULL DEFAULT 'Open';

UPDATE items SET status = 'Resolved' WHERE status = 'Returned';

ALTER TABLE items
    MODIFY status ENUM('Open', 'Resolved') NOT NULL DEFAULT 'Open',
    ADD COLUMN user_id INT NULL AFTER id,
    ADD COLUMN image_data LONGBLOB NULL,
    ADD COLUMN image_type VARCHAR(30) NULL,
    ADD INDEX idx_items_user_status (user_id, status),
    ADD INDEX idx_items_status_created (status, created_at),
    ADD CONSTRAINT fk_items_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
