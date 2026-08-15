package by.korchagin.planner.reminder.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import by.korchagin.planner.reminder.entity.ReminderDelivery;
import by.korchagin.planner.reminder.entity.ReminderDeliveryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderDeliveryRepository extends JpaRepository<ReminderDelivery, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ReminderDelivery> findFirstByStatusOrderByCreatedAtAsc(ReminderDeliveryStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ReminderDelivery> findByIdAndTelegramUserId(UUID id, long telegramUserId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			INSERT INTO reminder_deliveries (
			    id,
			    reminder_id,
			    telegram_user_id,
			    text,
			    status,
			    created_at,
			    scheduled_for,
			    version
			)
			SELECT
			    gen_random_uuid(),
			    reminder.id,
			    reminder.telegram_user_id,
			    reminder.text,
			    'PENDING',
			    :createdAt,
			    reminder.remind_at,
			    0
			FROM reminders reminder
			WHERE reminder.status = 'SCHEDULED'
			  AND reminder.remind_at <= :dueAt
			ON CONFLICT (reminder_id, scheduled_for) DO NOTHING
			""", nativeQuery = true)
	int enqueueDueReminders(@Param("dueAt") Instant dueAt, @Param("createdAt") Instant createdAt);
}
