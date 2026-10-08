CREATE TABLE IF NOT EXISTS parking_spaces (
  space_id BIGINT PRIMARY KEY,
  area VARCHAR(50) NOT NULL,
  type VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  version INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  KEY idx_parking_spaces_status (status),
  KEY idx_parking_spaces_area (area),
  KEY idx_parking_spaces_area_status (area, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
