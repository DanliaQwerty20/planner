package by.korchagin.planner.reminder.repository;

import java.util.Optional;
import java.util.UUID;

import by.korchagin.planner.reminder.entity.ReminderConversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderConversationRepository extends JpaRepository<ReminderConversation, UUID> {

	Optional<ReminderConversation> findByTelegramUserIdAndChatId(long telegramUserId, long chatId);

	void deleteByTelegramUserIdAndChatId(long telegramUserId, long chatId);
}
