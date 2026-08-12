CREATE TABLE reminder_drafts (
    id UUID PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL,
    text VARCHAR(500) NOT NULL,
    remind_at TIMESTAMP WITH TIME ZONE NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reminder_id UUID UNIQUE REFERENCES reminders(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE INDEX idx_reminder_drafts_user_created
    ON reminder_drafts (telegram_user_id, created_at DESC);
