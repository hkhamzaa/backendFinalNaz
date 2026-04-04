-- One-time: admin / admin@123 (BCrypt via PasswordGenerator)
INSERT IGNORE INTO roles (name, description) VALUES ('ROLE_ADMIN', 'Administrator');

INSERT INTO users (username, email, password, first_name, last_name, created_at, updated_at)
VALUES (
  'admin',
  'admin@localhost',
  '$2a$10$TnJetJ6Q4m5ZfmAecorYVeJcufF2IV7MpuOCJPVCfvFtAER.R8c72',
  'Admin',
  'User',
  NOW(6),
  NOW(6)
)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  email = VALUES(email),
  first_name = VALUES(first_name),
  last_name = VALUES(last_name),
  updated_at = NOW(6);

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.username = 'admin';
