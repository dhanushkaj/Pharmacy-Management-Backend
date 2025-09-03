-- Pharmacy Management DB Schema

-- Categories
CREATE TABLE categories (
    category_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

-- Products
CREATE TABLE products (
    product_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category_id INT REFERENCES categories(category_id),
    supplier_id INT REFERENCES suppliers(supplier_id),
    price DECIMAL(10,2) NOT NULL,
    stock INT DEFAULT 0,
    bin_location VARCHAR(50)
);

-- Suppliers
CREATE TABLE suppliers (
    supplier_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    contact_info TEXT
);

-- Customers
CREATE TABLE customers (
    customer_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT
);

-- Billing (Sales)
CREATE TABLE sales (
    sale_id SERIAL PRIMARY KEY,
    customer_id INT REFERENCES customers(customer_id),
    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2)
);

CREATE TABLE sale_items (
    sale_item_id SERIAL PRIMARY KEY,
    sale_id INT REFERENCES sales(sale_id),
    product_id INT REFERENCES products(product_id),
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL
);

-- Purchase Orders
CREATE TABLE purchase_orders (
    po_id SERIAL PRIMARY KEY,
    supplier_id INT REFERENCES suppliers(supplier_id),
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20)
);

CREATE TABLE purchase_order_items (
    po_item_id SERIAL PRIMARY KEY,
    po_id INT REFERENCES purchase_orders(po_id),
    product_id INT REFERENCES products(product_id),
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL
);

-- GRN (Goods Receipt Note)
CREATE TABLE grn (
    grn_id SERIAL PRIMARY KEY,
    po_id INT REFERENCES purchase_orders(po_id),
    received_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20)
);

-- Users for authentication
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Roles (optional)
CREATE TABLE roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(20) UNIQUE NOT NULL,
    description TEXT
);

-- User-Role mapping (optional)
CREATE TABLE user_roles (
    user_id INT REFERENCES users(user_id),
    role_id INT REFERENCES roles(role_id),
    PRIMARY KEY (user_id, role_id)
);

-- Alerts/Reports
CREATE TABLE alerts (
    alert_id SERIAL PRIMARY KEY,
    product_id INT REFERENCES products(product_id),
    alert_type VARCHAR(50),
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sequences (if not using SERIAL)
CREATE SEQUENCE product_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE user_seq START WITH 1 INCREMENT BY 1;

-- Insert default roles
INSERT INTO roles (role_name, description) VALUES
('admin', 'Administrator with full access'),
('pharmacist', 'Pharmacist with limited access'),
('manager', 'Manager with reporting access');

-- Insert a test user (password should be hashed in backend)
INSERT INTO users (username, password_hash, role)
VALUES ('admin', '$2b$10$examplehashstring', 'admin');

-- Example stored procedure for adding a sale (PostgreSQL)
CREATE OR REPLACE FUNCTION add_sale(
    p_customer_id INT,
    p_items JSON
) RETURNS INT AS $$
DECLARE
    v_sale_id INT;
BEGIN
    INSERT INTO sales (customer_id, sale_date, total_amount)
    VALUES (p_customer_id, CURRENT_TIMESTAMP, 0)
    RETURNING sale_id INTO v_sale_id;
    -- Loop through items and insert into sale_items
    -- p_items: [{product_id: 1, quantity: 2, price: 100.00}, ...]
    -- This part is pseudo-code, actual implementation depends on DB
    -- Update product stock as well
    RETURN v_sale_id;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE documents (
    document_id SERIAL PRIMARY KEY,
    grn_id INT REFERENCES grn(grn_id),           -- Link to GRN
    sale_id INT REFERENCES sales(sale_id),        -- Link to Sales (optional)
    product_id INT REFERENCES products(product_id), -- Link to Product (optional)
    document_type VARCHAR(50),                    -- e.g., 'receipt', 'bill', 'invoice'
    file_path VARCHAR(255),                       -- Path to file in filesystem or URL
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by INT REFERENCES users(user_id)     -- Who uploaded
);

