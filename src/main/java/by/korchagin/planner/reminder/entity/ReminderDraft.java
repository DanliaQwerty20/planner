package by.korchagin.planner.reminder.entity;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

import by.korchagin.planner.reminder.exception.InvalidReminderException;
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
@Table(name = "reminder_drafts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminderDraft {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "telegram_user_id", nullable = false, updatable = false)
	private long telegramUserId;

	@Column(nullable = false, updatable = false, length = Reminder.MAX_TEXT_LENGTH)
	private String text;

	@Column(name = "remind_at", nullable = false, updatable = false)
	private Instant remindAt;

	@Column(name = "time_zone", nullable = false, updatable = false, length = 64)
	private String timeZone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ReminderDraftStatus status;

	@Column(name = "reminder_id", unique = true)
	private UUID reminderId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "confirmed_at")
	private Instant confirmedAt;

	@Version
	private Long version;

	public static ReminderDraft create(
			long telegramUserId,
			String text,
			Instant remindAt,
			ZoneId timeZone) {
		if (telegramUserId <= 0) {
			throw new InvalidReminderException("Telegram user id must be positive");
		}
		if (text == null || text.isBlank()) {
			throw new InvalidReminderException("Reminder text must not be blank");
		}
		if (text.length() > Reminder.MAX_TEXT_LENGTH) {
			throw new InvalidReminderException("Reminder text must not exceed 500 characters");
		}

		var draft = new ReminderDraft();
		draft.telegramUserId = telegramUserId;
		draft.text = text;
		draft.remindAt = Objects.requireNonNull(remindAt, "remindAt must not be null");
		draft.timeZone = Objects.requireNonNull(timeZone, "timeZone must not be null").getId();
		draft.status = ReminderDraftStatus.PENDING;
		return draft;
	}

	public boolean isConfirmed() {
		return status == ReminderDraftStatus.CONFIRMED;
	}

	public ZoneId getTimeZone() {
		return ZoneId.of(timeZone);
	}

	public void confirm(UUID reminderId, Instant confirmedAt) {
		if (isConfirmed()) {
			return;
		}

		this.reminderId = Objects.requireNonNull(reminderId, "reminderId must not be null");
		this.confirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
		status = ReminderDraftStatus.CONFIRMED;
	}
}
