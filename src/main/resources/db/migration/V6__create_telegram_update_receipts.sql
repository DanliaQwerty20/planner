CREATE TABLE telegram_update_receipts (
    update_id BIGINT PRIMARY KEY,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL
);
