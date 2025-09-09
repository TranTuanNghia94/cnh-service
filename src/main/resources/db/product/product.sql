-- Categories table
CREATE TABLE categories (
    id uuid PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    unit VARCHAR(100),
    description TEXT,
    parent_id uuid REFERENCES categories(id),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- Products table
CREATE TABLE products (
    id uuid PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    barcode VARCHAR(100),
    category_id uuid REFERENCES categories(id),
    unit_1 VARCHAR(20),
    unit_2 VARCHAR(20),
    price DECIMAL(15,2),
    tax DECIMAL(15,2) NOT NULL DEFAULT 0,
    misa_code VARCHAR(100),
    cost_price DECIMAL(15,2),
    image_url VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);