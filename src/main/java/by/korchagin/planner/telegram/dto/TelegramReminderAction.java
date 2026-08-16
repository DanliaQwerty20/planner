package by.korchagin.planner.telegram.dto;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record TelegramReminderAction(Type type, UUID targetId) {

	private static final String PREFIX = "reminder:";

	public TelegramReminderAction {
		Objects.requireNonNull(type, "type must not be null");
		Objects.requireNonNull(targetId, "targetId must not be null");
	}

	public static TelegramReminderAction complete(UUID reminderId) {
		return new TelegramReminderAction(Type.COMPLETE, reminderId);
	}

	public static TelegramReminderAction snooze(UUID deliveryId) {
		return new TelegramReminderAction(Type.SNOOZE, deliveryId);
	}

	public static TelegramReminderAction cancel(UUID reminderId) {
		return new TelegramReminderAction(Type.CANCEL, reminderId);
	}

	public static Optional<TelegramReminderAction> parse(String data) {
		if (data == null || !data.startsWith(PREFIX)) {
			return Optional.empty();
		}

		var separatorIndex = data.indexOf(':', PREFIX.length());
		if (separatorIndex < 0) {
			return Optional.empty();
		}

		var type = Type.fromValue(data.substring(PREFIX.length(), separatorIndex));
		if (type.isEmpty()) {
			return Optional.empty();
		}

		var targetId = UUID.fromString(data.substring(separatorIndex + 1));
		return Optional.of(new TelegramReminderAction(type.orElseThrow(), targetId));
	}

	public String data() {
		return PREFIX + type.value + ":" + targetId;
	}

	public enum Type {
		COMPLETE("complete"),
		SNOOZE("snooze"),
		CANCEL("cancel");

		private final String value;

		Type(String value) {
			this.value = value;
		}

		private static Optional<Type> fromValue(String value) {
			for (var type : values()) {
				if (type.value.equals(value)) {
					return Optional.of(type);
				}
			}
			return Optional.empty();
		}
	}
}
