CREATE TABLE reminder_deliveries (
    reminder_id UUID PRIMARY KEY REFERENCES reminders(id) ON DELETE CASCADE,
    telegram_user_id BIGINT NOT NULL,
    text VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE INDEX idx_reminder_deliveries_pending
    ON reminder_deliveries (status, created_at);
