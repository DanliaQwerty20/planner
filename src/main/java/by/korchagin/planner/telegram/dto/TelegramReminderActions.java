package by.korchagin.planner.telegram.dto;

import java.util.Objects;

public record TelegramReminderActions(
		String completionData,
		String snoozeData,
		String cancellationData) {

	public TelegramReminderActions {
		Objects.requireNonNull(completionData, "completionData must not be null");
		Objects.requireNonNull(snoozeData, "snoozeData must not be null");
		Objects.requireNonNull(cancellationData, "cancellationData must not be null");
	}
}
