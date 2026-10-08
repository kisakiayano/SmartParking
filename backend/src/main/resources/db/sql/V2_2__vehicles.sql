CREATE TABLE IF NOT EXISTS vehicles (
  vehicle_id BIGINT PRIMARY KEY,
  plate_number VARCHAR(20) NOT NULL,
  vehicle_type VARCHAR(30) NOT NULL,
  user_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_vehicles_plate_number (plate_number),
  KEY idx_vehicles_user_id (user_id),
  CONSTRAINT fk_vehicles_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
