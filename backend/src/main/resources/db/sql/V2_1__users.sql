CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(100) NOT NULL,
  name VARCHAR(64) NOT NULL,
  phone VARCHAR(20) NULL,
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  token_version BIGINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_phone (phone),
  KEY idx_users_role (role),
  KEY idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Foreign keys:
-- users is the parent table for vehicles, reservations and operation_logs in later phases.

-- Default administrator data should be inserted per environment with a BCrypt password hash.
-- Do not store production administrator passwords in this repository.
