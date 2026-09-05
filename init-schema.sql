-- Create roles table
CREATE TABLE IF NOT EXISTS rdp_roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL
);

-- Create users table  
CREATE TABLE IF NOT EXISTS rdp_users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    session_code VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create user-role junction table
CREATE TABLE IF NOT EXISTS rdp_user_roles (
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES rdp_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES rdp_roles(role_id) ON DELETE CASCADE
);

-- Insert roles
INSERT INTO rdp_roles (role_name) VALUES ('ADMIN') ON CONFLICT (role_name) DO NOTHING;
INSERT INTO rdp_roles (role_name) VALUES ('PHARMACIST') ON CONFLICT (role_name) DO NOTHING;
INSERT INTO rdp_roles (role_name) VALUES ('MANAGER') ON CONFLICT (role_name) DO NOTHING;

-- Create admin user with password 'admin' (bcrypt hash)
-- Password: admin -> $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36gBS/O.
INSERT INTO rdp_users (username, password_hash, email, is_active) 
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36gBS/O.', 'admin@pharmacy.local', true)
ON CONFLICT (username) DO NOTHING;

-- Assign ADMIN role to admin user
INSERT INTO rdp_user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM rdp_users u, rdp_roles r
WHERE u.username = 'admin' AND r.role_name = 'ADMIN'
ON CONFLICT DO NOTHING;
