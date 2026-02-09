CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- Create index for faster username lookups
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Insert default admin user (password will be set in V2)
-- Using a placeholder password that will be updated after application starts
INSERT INTO users (username, password, role)
VALUES ('ffff', '$2a$10$e1qZB6SnrFCVV6kFzA5Kg.DB88zJgoYAqysNPTh3vMmAqoEk/tGQi', 'ADMIN')
ON CONFLICT (username) DO NOTHING;