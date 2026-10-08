SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS fee_rules (
  rule_id BIGINT PRIMARY KEY,
  rule_name VARCHAR(64) NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  free_minute INT NOT NULL,
  max_daily DECIMAL(10,2) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  KEY idx_fee_rules_enabled (enabled),
  UNIQUE KEY uk_fee_rules_enabled ((IF(enabled = 1, 1, NULL)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO fee_rules (
  rule_id,
  rule_name,
  unit_price,
  free_minute,
  max_daily,
  enabled,
  create_time,
  update_time
)
SELECT
  1001,
  '默认计费规则',
  5.00,
  15,
  60.00,
  1,
  NOW(),
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM fee_rules
);
