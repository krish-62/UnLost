CREATE DATABASE IF NOT EXISTS unlost;
USE unlost;

CREATE TABLE IF NOT EXISTS items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('Lost', 'Found') NOT NULL,
    name VARCHAR(80) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    location VARCHAR(120) NOT NULL,
    item_date DATE NOT NULL,
    contact VARCHAR(120) NOT NULL,
    status ENUM('Open', 'Returned') NOT NULL DEFAULT 'Open',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
