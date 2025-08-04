--liquibase formatted sql

--changeset vpn-management:create-sales-records-table
CREATE TABLE sales_records (
    record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID NOT NULL,
    sale_date DATE NOT NULL,
    revenue DECIMAL(10,2) NOT NULL DEFAULT 0,
    new_subscribers INTEGER NOT NULL DEFAULT 0,
    refunds INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(seller_id, sale_date)
);

--rollback DROP TABLE sales_records;

--changeset vpn-management:create-sales-records-indexes
CREATE INDEX idx_sales_records_seller_id ON sales_records(seller_id);
CREATE INDEX idx_sales_records_sale_date ON sales_records(sale_date);
CREATE INDEX idx_sales_records_seller_date ON sales_records(seller_id, sale_date);

--rollback DROP INDEX idx_sales_records_seller_id; DROP INDEX idx_sales_records_sale_date; DROP INDEX idx_sales_records_seller_date;