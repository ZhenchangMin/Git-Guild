-- GitGuild local development seed data.
-- Creates a demo ADMIN account for local frontend/backend testing only.
--
-- Login email: admin@gitguild.local
-- Password: admin123
--
-- Do not run this seed in production environments.

USE gitguild;

INSERT IGNORE INTO users (
    username,
    email,
    password_hash,
    role,
    status,
    token_version,
    created_at,
    updated_at
) VALUES (
    'admin',
    'admin@gitguild.local',
    '$2a$10$Qd5H2WYJeBvIPgSo9tVf9eoIglSPUzsAnJH8Be/tDVUX/JNyD4XyC',
    'ADMIN',
    'ACTIVE',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
