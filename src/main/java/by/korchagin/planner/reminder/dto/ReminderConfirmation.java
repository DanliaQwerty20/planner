package by.korchagin.planner.reminder.dto;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

public record ReminderConfirmation(
		UUID reminderId,
		String text,
		Instant remindAt,
		ZoneId timeZone,
		boolean created) {
}
