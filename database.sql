CREATE DATABASE IF NOT EXISTS unlost;
USE unlost;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    type ENUM('Lost', 'Found') NOT NULL,
    name VARCHAR(80) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    location VARCHAR(120) NOT NULL,
    item_date DATE NOT NULL,
    contact VARCHAR(120) NOT NULL,
    image_data LONGBLOB NULL,
    image_type VARCHAR(30) NULL,
    status ENUM('Open', 'Resolved') NOT NULL DEFAULT 'Open',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_items_user_status (user_id, status),
    INDEX idx_items_status_created (status, created_at),
    CONSTRAINT fk_items_user FOREIGN KEY (user_id) REFERENCES users(id)
);
