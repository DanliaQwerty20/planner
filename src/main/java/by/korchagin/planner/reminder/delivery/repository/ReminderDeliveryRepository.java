package by.korchagin.planner.reminder.delivery.repository;

import java.time.Instant;
import java.util.UUID;

import by.korchagin.planner.reminder.delivery.entity.ReminderDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReminderDeliveryRepository extends JpaRepository<ReminderDelivery, UUID> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			INSERT INTO reminder_deliveries (
			    reminder_id,
			    telegram_user_id,
			    text,
			    status,
			    created_at,
			    version
			)
			SELECT
			    reminder.id,
			    reminder.telegram_user_id,
			    reminder.text,
			    'PENDING',
			    :createdAt,
			    0
			FROM reminders reminder
			WHERE reminder.status = 'SCHEDULED'
			  AND reminder.remind_at <= :dueAt
			  AND NOT EXISTS (
			      SELECT 1
			      FROM reminder_deliveries delivery
			      WHERE delivery.reminder_id = reminder.id
			  )
			ON CONFLICT (reminder_id) DO NOTHING
			""", nativeQuery = true)
	int enqueueDueReminders(@Param("dueAt") Instant dueAt, @Param("createdAt") Instant createdAt);
}
