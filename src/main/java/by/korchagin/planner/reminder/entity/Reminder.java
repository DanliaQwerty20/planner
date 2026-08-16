package by.korchagin.planner.reminder.entity;

import java.time.Instant;
import java.util.UUID;

import by.korchagin.planner.reminder.exception.InvalidReminderException;
import by.korchagin.planner.reminder.exception.ReminderAlreadyCancelledException;
import by.korchagin.planner.reminder.exception.ReminderAlreadyCompletedException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "reminders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder {

	public static final int MAX_TEXT_LENGTH = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "telegram_user_id", nullable = false, updatable = false)
	private long telegramUserId;

	@Column(nullable = false, length = MAX_TEXT_LENGTH)
	private String text;

	@Column(name = "remind_at", nullable = false)
	private Instant remindAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ReminderStatus status;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Version
	private Long version;

	public static Reminder schedule(long telegramUserId, String text, Instant remindAt, Instant createdAt) {
		validateTelegramUserId(telegramUserId);
		var normalizedText = normalizeText(text);
		validateFutureTime(remindAt, createdAt);

		var reminder = new Reminder();
		reminder.telegramUserId = telegramUserId;
		reminder.text = normalizedText;
		reminder.remindAt = remindAt;
		reminder.status = ReminderStatus.SCHEDULED;
		return reminder;
	}

	public void complete(Instant completionTime) {
		if (status == ReminderStatus.COMPLETED) {
			return;
		}
		if (status == ReminderStatus.CANCELLED) {
			throw new ReminderAlreadyCancelledException("Cancelled reminder cannot be completed");
		}
		if (completionTime == null) {
			throw new InvalidReminderException("Completion time must not be null");
		}

		status = ReminderStatus.COMPLETED;
		completedAt = completionTime;
	}

	public void cancel(Instant cancellationTime) {
		if (status == ReminderStatus.CANCELLED) {
			return;
		}
		if (status == ReminderStatus.COMPLETED) {
			throw new ReminderAlreadyCompletedException("Completed reminder cannot be cancelled");
		}
		if (cancellationTime == null) {
			throw new InvalidReminderException("Cancellation time must not be null");
		}

		status = ReminderStatus.CANCELLED;
		cancelledAt = cancellationTime;
	}

	public void reschedule(Instant newRemindAt, Instant currentTime) {
		if (status == ReminderStatus.COMPLETED) {
			throw new ReminderAlreadyCompletedException("Completed reminder cannot be rescheduled");
		}
		if (status == ReminderStatus.CANCELLED) {
			throw new ReminderAlreadyCancelledException("Cancelled reminder cannot be rescheduled");
		}

		validateFutureTime(newRemindAt, currentTime);
		remindAt = newRemindAt;
	}

	private static void validateTelegramUserId(long telegramUserId) {
		if (telegramUserId <= 0) {
			throw new InvalidReminderException("Telegram user id must be positive");
		}
	}

	private static String normalizeText(String text) {
		if (text == null || text.isBlank()) {
			throw new InvalidReminderException("Reminder text must not be blank");
		}

		var normalizedText = text.strip();
		if (normalizedText.length() > MAX_TEXT_LENGTH) {
			throw new InvalidReminderException("Reminder text must not exceed 500 characters");
		}
		return normalizedText;
	}

	private static void validateFutureTime(Instant remindAt, Instant currentTime) {
		if (remindAt == null || currentTime == null || !remindAt.isAfter(currentTime)) {
			throw new InvalidReminderException("Reminder time must be in the future");
		}
	}
}
