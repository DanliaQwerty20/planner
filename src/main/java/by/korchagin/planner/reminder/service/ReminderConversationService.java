package by.korchagin.planner.reminder.service;

import by.korchagin.planner.reminder.dto.ReminderClarification;
import by.korchagin.planner.reminder.dto.ReminderInterpretationResult;
import by.korchagin.planner.reminder.entity.ReminderConversation;
import by.korchagin.planner.reminder.repository.ReminderConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager")
public class ReminderConversationService {

	private final ReminderTextInterpreter reminderTextInterpreter;
	private final ReminderConversationRepository reminderConversationRepository;

	public ReminderInterpretationResult interpret(
			long telegramUserId,
			long chatId,
			String message) {
		var conversation = reminderConversationRepository
				.findByTelegramUserIdAndChatId(telegramUserId, chatId)
				.orElse(null);
		String context;
		try {
			context = conversation == null ? message.strip() : conversation.append(message);
		}
		catch (IllegalArgumentException exception) {
			reminderConversationRepository.delete(conversation);
			return new ReminderClarification(
					"Я запутался в длинном диалоге. Отправь напоминание заново одним сообщением.",
					false);
		}
		var result = reminderTextInterpreter.interpret(context);

		if (result instanceof ReminderClarification clarification) {
			if (!clarification.retainContext()) {
				if (conversation != null) {
					reminderConversationRepository.delete(conversation);
				}
				return result;
			}
			if (conversation == null) {
				reminderConversationRepository.save(
						ReminderConversation.start(telegramUserId, chatId, context));
			}
			return result;
		}

		if (conversation != null) {
			reminderConversationRepository.delete(conversation);
		}
		return result;
	}

	public void reset(long telegramUserId, long chatId) {
		reminderConversationRepository.deleteByTelegramUserIdAndChatId(telegramUserId, chatId);
	}
}
