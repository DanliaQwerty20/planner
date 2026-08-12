package by.korchagin.planner.telegram.service;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import by.korchagin.planner.reminder.dto.ReminderConfirmation;
import by.korchagin.planner.reminder.dto.ReminderInterpretation;
import by.korchagin.planner.reminder.service.ReminderDraftService;
import by.korchagin.planner.reminder.service.ReminderTextInterpreter;
import by.korchagin.planner.telegram.client.TelegramClient;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramUpdateService {

	private static final DateTimeFormatter DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
	private static final String CONFIRM_CALLBACK_PREFIX = "reminder:confirm:";

	private final ReminderTextInterpreter reminderTextInterpreter;
	private final ReminderDraftService reminderDraftService;
	private final TelegramClient telegramClient;

	public void handle(TelegramUpdate update) {
		if (update.message() != null && update.message().text() != null) {
			handleTextMessage(update.message());
			return;
		}
		if (isConfirmationCallback(update.callbackQuery())) {
			handleConfirmation(update.callbackQuery());
			return;
		}

		throw new IllegalArgumentException("Unsupported Telegram update: " + update.updateId());
	}

	private boolean isConfirmationCallback(TelegramUpdate.TelegramCallbackQuery callbackQuery) {
		return callbackQuery != null
				&& callbackQuery.data() != null
				&& callbackQuery.data().startsWith(CONFIRM_CALLBACK_PREFIX);
	}

	private void handleTextMessage(TelegramUpdate.TelegramMessage message) {
		var interpretation = reminderTextInterpreter.interpret(message.text());
		var draft = reminderDraftService.create(message.from().id(), interpretation);
		telegramClient.sendConfirmation(
				message.chat().id(),
				formatPreview(interpretation),
				CONFIRM_CALLBACK_PREFIX + draft.getId());
	}

	private void handleConfirmation(TelegramUpdate.TelegramCallbackQuery callbackQuery) {
		var draftId = UUID.fromString(callbackQuery.data().substring(CONFIRM_CALLBACK_PREFIX.length()));
		var confirmation = reminderDraftService.confirm(draftId, callbackQuery.from().id());
		telegramClient.sendMessage(
				callbackQuery.message().chat().id(),
				formatConfirmation(confirmation));
	}

	private String formatPreview(ReminderInterpretation interpretation) {
		var localTime = interpretation.remindAt().atZone(interpretation.timeZone());
		return "Проверь напоминание:\n%s — %s"
				.formatted(DATE_TIME_FORMATTER.format(localTime), interpretation.text());
	}

	private String formatConfirmation(ReminderConfirmation confirmation) {
		var localTime = confirmation.remindAt().atZone(confirmation.timeZone());
		return "Напоминание создано: %s — %s"
				.formatted(DATE_TIME_FORMATTER.format(localTime), confirmation.text());
	}
}
