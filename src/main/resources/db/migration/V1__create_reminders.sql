CREATE TABLE reminders (
    id UUID PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL,
    text VARCHAR(500) NOT NULL,
    remind_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE INDEX idx_reminders_scheduled
    ON reminders (status, remind_at);

CREATE INDEX idx_reminders_user
    ON reminders (telegram_user_id, created_at DESC);
