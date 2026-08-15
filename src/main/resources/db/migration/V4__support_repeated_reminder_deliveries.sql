ALTER TABLE reminder_deliveries
    ADD COLUMN id UUID;

UPDATE reminder_deliveries
SET id = reminder_id;

ALTER TABLE reminder_deliveries
    ALTER COLUMN id SET NOT NULL,
    DROP CONSTRAINT reminder_deliveries_pkey,
    ADD CONSTRAINT reminder_deliveries_pkey PRIMARY KEY (id),
    ADD COLUMN scheduled_for TIMESTAMP WITH TIME ZONE,
    ADD COLUMN snoozed_until TIMESTAMP WITH TIME ZONE;

UPDATE reminder_deliveries delivery
SET scheduled_for = CASE
    WHEN reminder.remind_at <= delivery.created_at THEN reminder.remind_at
    ELSE delivery.created_at
END
FROM reminders reminder
WHERE reminder.id = delivery.reminder_id;

ALTER TABLE reminder_deliveries
    ALTER COLUMN scheduled_for SET NOT NULL,
    ADD CONSTRAINT uq_reminder_deliveries_schedule UNIQUE (reminder_id, scheduled_for);
