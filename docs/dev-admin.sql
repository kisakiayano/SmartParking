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
SELECT
  2063922857607946242,
  'admin',
  '$2a$10$hwAy6ZSRwyJuQiCST/S/p.WnqaOLnC.d1q/BjlbesPsaJQIAYYZG2',
  'Administrator',
  NULL,
  'ADMIN',
  'ENABLED',
  NOW(),
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE username = 'admin'
);
