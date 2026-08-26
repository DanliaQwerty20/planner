package by.korchagin.planner.telegram.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "telegram_update_receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramUpdateReceipt {

	@Id
	@Column(name = "update_id", nullable = false, updatable = false)
	private Long updateId;

	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt;
}
