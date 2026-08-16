package by.korchagin.planner.reminder.repository;

import java.util.Optional;
import java.util.UUID;

import by.korchagin.planner.reminder.entity.Reminder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Reminder> findByIdAndTelegramUserId(UUID id, long telegramUserId);
}
