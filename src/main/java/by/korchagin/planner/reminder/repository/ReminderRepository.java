package by.korchagin.planner.reminder.repository;

import java.util.Optional;
import java.util.UUID;

import by.korchagin.planner.reminder.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

	Optional<Reminder> findByIdAndTelegramUserId(UUID id, long telegramUserId);
}
