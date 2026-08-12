package by.korchagin.planner.reminder.dto;

import java.time.Instant;
import java.time.ZoneId;

public record ReminderInterpretation(
		String text,
		Instant remindAt,
		ZoneId timeZone) implements ReminderInterpretationResult {
}
