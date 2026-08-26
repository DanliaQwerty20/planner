package by.korchagin.planner.reminder.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
		name = "reminder_conversations",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_reminder_conversations_user_chat",
				columnNames = {"telegram_user_id", "chat_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReminderConversation {

	public static final int MAX_CONTEXT_LENGTH = 2000;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "telegram_user_id", nullable = false, updatable = false)
	private long telegramUserId;

	@Column(name = "chat_id", nullable = false, updatable = false)
	private long chatId;

	@Column(name = "context_text", nullable = false, length = MAX_CONTEXT_LENGTH)
	private String contextText;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private Long version;

	public static ReminderConversation start(long telegramUserId, long chatId, String message) {
		var conversation = new ReminderConversation();
		conversation.telegramUserId = positive(telegramUserId, "telegramUserId");
		conversation.chatId = positive(chatId, "chatId");
		conversation.contextText = validContext(message);
		return conversation;
	}

	public String append(String message) {
		var nextMessage = Objects.requireNonNull(message, "message must not be null").strip();
		contextText = validContext(contextText + "\n" + nextMessage);
		return contextText;
	}

	private static long positive(long value, String name) {
		if (value <= 0) {
			throw new IllegalArgumentException(name + " must be positive");
		}
		return value;
	}

	private static String validContext(String value) {
		var context = Objects.requireNonNull(value, "context must not be null").strip();
		if (context.isBlank()) {
			throw new IllegalArgumentException("context must not be blank");
		}
		if (context.length() > MAX_CONTEXT_LENGTH) {
			throw new IllegalArgumentException("context must not exceed " + MAX_CONTEXT_LENGTH + " characters");
		}
		return context;
	}
}
