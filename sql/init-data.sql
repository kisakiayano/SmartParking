-- Smart Parking seed data.
-- Password for all seed users: Password@123
-- This script is repeatable for the seed ID ranges below.

SET NAMES utf8mb4;

SET @seed_user_start := 9001001;
SET @seed_vehicle_start := 9002001;
SET @seed_space_start := 9003001;
SET @seed_reservation_start := 9004001;
SET @seed_record_start := 9005001;
SET @seed_fee_rule_start := 9006001;

START TRANSACTION;

DELETE FROM plate_recognition_logs
WHERE record_id BETWEEN @seed_record_start AND @seed_record_start + 49;

DELETE FROM parking_records
WHERE record_id BETWEEN @seed_record_start AND @seed_record_start + 49;

DELETE FROM reservations
WHERE reservation_id BETWEEN @seed_reservation_start AND @seed_reservation_start + 19;

DELETE FROM vehicles
WHERE vehicle_id BETWEEN @seed_vehicle_start AND @seed_vehicle_start + 29;

DELETE FROM parking_spaces
WHERE space_id BETWEEN @seed_space_start AND @seed_space_start + 99;

DELETE FROM fee_rules
WHERE rule_id BETWEEN @seed_fee_rule_start AND @seed_fee_rule_start + 2;

DELETE FROM users
WHERE user_id BETWEEN @seed_user_start AND @seed_user_start + 9;

INSERT INTO users (
  user_id,
  username,
  password,
  name,
  phone,
  role,
  status,
  create_time,
  update_time
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 10
)
SELECT
  @seed_user_start + n - 1,
  CONCAT('seed_user_', LPAD(n, 2, '0')),
  '$2a$10$hwAy6ZSRwyJuQiCST/S/p.WnqaOLnC.d1q/BjlbesPsaJQIAYYZG2',
  CONCAT('Seed User ', LPAD(n, 2, '0')),
  CONCAT('1399000', LPAD(n, 4, '0')),
  'USER',
  'ENABLED',
  NOW(),
  NOW()
FROM seq;

INSERT INTO vehicles (
  vehicle_id,
  plate_number,
  vehicle_type,
  user_id,
  create_time,
  update_time
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 30
)
SELECT
  @seed_vehicle_start + n - 1,
  CONCAT('粤B', LPAD(80000 + n, 5, '0')),
  CASE
    WHEN n % 3 = 0 THEN 'NEW_ENERGY'
    WHEN n % 3 = 1 THEN 'SMALL'
    ELSE 'SUV'
  END,
  @seed_user_start + ((n - 1) % 10),
  NOW(),
  NOW()
FROM seq;

INSERT INTO parking_spaces (
  space_id,
  area,
  type,
  status,
  version,
  create_time,
  update_time
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 100
)
SELECT
  @seed_space_start + n - 1,
  ELT(FLOOR((n - 1) / 25) + 1, 'Seed-A', 'Seed-B', 'Seed-C', 'Seed-D'),
  CASE
    WHEN n % 10 = 0 THEN 'CHARGING'
    WHEN n % 5 = 0 THEN 'LARGE'
    ELSE 'STANDARD'
  END,
  CASE
    WHEN n BETWEEN 1 AND 10 THEN 'RESERVED'
    WHEN n BETWEEN 21 AND 35 THEN 'OCCUPIED'
    ELSE 'FREE'
  END,
  CASE
    WHEN n BETWEEN 1 AND 10 THEN 1
    WHEN n BETWEEN 21 AND 35 THEN 1
    ELSE 0
  END,
  NOW(),
  NOW()
FROM seq;

INSERT INTO reservations (
  reservation_id,
  user_id,
  vehicle_id,
  space_id,
  reserve_time,
  arrive_time,
  status,
  create_time,
  update_time
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 20
)
SELECT
  @seed_reservation_start + n - 1,
  @seed_user_start + ((n - 1) % 10),
  @seed_vehicle_start + n - 1,
  CASE
    WHEN n <= 10 THEN @seed_space_start + n - 1
    WHEN n <= 15 THEN @seed_space_start + 20 + n - 11
    ELSE @seed_space_start + 10 + n - 16
  END,
  DATE_SUB(NOW(), INTERVAL n HOUR),
  DATE_ADD(NOW(), INTERVAL n HOUR),
  CASE
    WHEN n BETWEEN 1 AND 10 THEN 'ACTIVE'
    WHEN n BETWEEN 11 AND 15 THEN 'USED'
    ELSE 'CANCELLED'
  END,
  DATE_SUB(NOW(), INTERVAL n HOUR),
  NOW()
FROM seq;

INSERT INTO parking_records (
  record_id,
  vehicle_id,
  space_id,
  entry_time,
  exit_time,
  duration,
  fee,
  status,
  create_time,
  update_time
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 50
)
SELECT
  @seed_record_start + n - 1,
  CASE
    WHEN n <= 15 THEN @seed_vehicle_start + 10 + n - 1
    ELSE @seed_vehicle_start + ((n - 1) % 30)
  END,
  CASE
    WHEN n <= 15 THEN @seed_space_start + 20 + n - 1
    ELSE @seed_space_start + 35 + n
  END,
  DATE_SUB(NOW(), INTERVAL (n + 60) MINUTE),
  CASE
    WHEN n <= 15 THEN NULL
    ELSE DATE_SUB(NOW(), INTERVAL n MINUTE)
  END,
  CASE
    WHEN n <= 15 THEN NULL
    ELSE 60
  END,
  CASE
    WHEN n <= 15 THEN NULL
    ELSE 5.00
  END,
  CASE
    WHEN n <= 15 THEN 'PARKING'
    ELSE 'FINISHED'
  END,
  DATE_SUB(NOW(), INTERVAL (n + 60) MINUTE),
  NOW()
FROM seq;

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
VALUES
  (@seed_fee_rule_start, '种子默认计费规则', 5.00, 15, 60.00, 0, NOW(), NOW()),
  (@seed_fee_rule_start + 1, '种子夜间计费规则', 3.00, 30, 40.00, 0, NOW(), NOW()),
  (@seed_fee_rule_start + 2, '种子节假日计费规则', 8.00, 10, 80.00, 0, NOW(), NOW());

COMMIT;
