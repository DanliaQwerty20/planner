package by.korchagin.planner.telegram.repository;

import java.time.Instant;

import by.korchagin.planner.telegram.entity.TelegramUpdateReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelegramUpdateReceiptRepository extends JpaRepository<TelegramUpdateReceipt, Long> {

	@Modifying
	@Query(value = """
			INSERT INTO telegram_update_receipts (update_id, received_at)
			VALUES (:updateId, :receivedAt)
			ON CONFLICT (update_id) DO NOTHING
			""", nativeQuery = true)
	int claim(@Param("updateId") long updateId, @Param("receivedAt") Instant receivedAt);
}
