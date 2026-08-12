package by.korchagin.planner.reminder.repository;

import java.util.Optional;
import java.util.UUID;

import by.korchagin.planner.reminder.entity.ReminderDraft;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ReminderDraftRepository extends JpaRepository<ReminderDraft, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ReminderDraft> findByIdAndTelegramUserId(UUID id, long telegramUserId);
}
