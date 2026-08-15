package by.korchagin.planner.reminder.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reminder_deliveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminderDelivery {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "reminder_id", nullable = false, updatable = false)
	private UUID reminderId;

	@Column(name = "telegram_user_id", nullable = false, updatable = false)
	private long telegramUserId;

	@Column(nullable = false, updatable = false, length = 500)
	private String text;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ReminderDeliveryStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "scheduled_for", nullable = false, updatable = false)
	private Instant scheduledFor;

	@Column(name = "sent_at")
	private Instant sentAt;

	@Column(name = "snoozed_until")
	private Instant snoozedUntil;

	@Version
	private Long version;

	public void markSent(Instant sentAt) {
		if (status != ReminderDeliveryStatus.PENDING) {
			throw new IllegalStateException("Only pending delivery can be marked as sent");
		}

		this.sentAt = Objects.requireNonNull(sentAt, "sentAt must not be null");
		status = ReminderDeliveryStatus.SENT;
	}

	public void markSnoozedUntil(Instant snoozedUntil) {
		if (status != ReminderDeliveryStatus.SENT) {
			throw new IllegalStateException("Only sent delivery can be snoozed");
		}
		if (this.snoozedUntil != null) {
			return;
		}

		this.snoozedUntil = Objects.requireNonNull(snoozedUntil, "snoozedUntil must not be null");
	}
}
