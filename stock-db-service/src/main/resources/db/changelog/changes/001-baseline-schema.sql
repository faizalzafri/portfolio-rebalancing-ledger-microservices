-- Create Quotes Table
CREATE TABLE quotes (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    quote VARCHAR(255) NOT NULL
);

-- Create Portfolios Table
CREATE TABLE portfolios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL
);

-- Create Portfolio Target Allocation Table
CREATE TABLE portfolio_target_allocation (
    portfolio_id BIGINT NOT NULL,
    asset_symbol VARCHAR(255) NOT NULL,
    target_weight NUMERIC(19, 2) NOT NULL,
    PRIMARY KEY (portfolio_id, asset_symbol),
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id)
);

-- Create Transactions Table
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    symbol VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    quantity NUMERIC(12, 4) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    timestamp TIMESTAMP NOT NULL
);

-- Create Tax Lots Table
CREATE TABLE tax_lots (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    symbol VARCHAR(255) NOT NULL,
    original_quantity NUMERIC(12, 4) NOT NULL,
    remaining_quantity NUMERIC(12, 4) NOT NULL,
    purchase_price NUMERIC(12, 2) NOT NULL,
    purchase_date TIMESTAMP NOT NULL
);

-- Create Trade Suggestions Table
CREATE TABLE trade_suggestions (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL,
    symbol VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    quantity NUMERIC(12, 4) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
