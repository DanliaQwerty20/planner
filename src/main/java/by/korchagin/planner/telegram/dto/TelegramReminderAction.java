package by.korchagin.planner.telegram.dto;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record TelegramReminderAction(UUID reminderId) {

	private static final String COMPLETE_PREFIX = "reminder:complete:";

	public TelegramReminderAction {
		Objects.requireNonNull(reminderId, "reminderId must not be null");
	}

	public static TelegramReminderAction complete(UUID reminderId) {
		return new TelegramReminderAction(reminderId);
	}

	public static Optional<TelegramReminderAction> parseCompletion(String data) {
		if (data == null || !data.startsWith(COMPLETE_PREFIX)) {
			return Optional.empty();
		}
		return Optional.of(complete(UUID.fromString(data.substring(COMPLETE_PREFIX.length()))));
	}

	public String data() {
		return COMPLETE_PREFIX + reminderId;
	}
}
