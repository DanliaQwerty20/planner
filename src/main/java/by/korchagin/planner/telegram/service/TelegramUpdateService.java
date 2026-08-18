package by.korchagin.planner.telegram.service;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import by.korchagin.planner.reminder.dto.ReminderConfirmation;
import by.korchagin.planner.reminder.dto.ReminderClarification;
import by.korchagin.planner.reminder.dto.ReminderInterpretation;
import by.korchagin.planner.reminder.service.ReminderCancellationService;
import by.korchagin.planner.reminder.service.ReminderDraftService;
import by.korchagin.planner.reminder.service.ReminderService;
import by.korchagin.planner.reminder.service.ReminderSnoozeService;
import by.korchagin.planner.reminder.service.ReminderTextInterpreter;
import by.korchagin.planner.telegram.client.TelegramClient;
import by.korchagin.planner.telegram.dto.TelegramReminderAction;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import by.korchagin.planner.voice.service.SpeechTranscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUpdateService {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
	private static final String CONFIRM_CALLBACK_PREFIX = "reminder:confirm:";
	private static final Duration SNOOZE_DELAY = Duration.ofHours(1);
	private static final String START_COMMAND = "/start";
	private static final String HELP_COMMAND = "/help";
	private static final String START_MESSAGE = "Привет! Я помогу не забыть важное.\n"
			+ "Отправь напоминание текстом, например: «завтра в 15:00 покормить кота».";
	private static final String HELP_MESSAGE = "Напиши одним сообщением, что и когда напомнить.\n"
			+ "Например: «в пятницу в 18:30 купить корм коту».\n"
			+ "Перед созданием я покажу дату и текст для подтверждения.";
	private static final String VOICE_DISABLED_MESSAGE =
			"Голосовые сообщения пока не поддерживаются. Отправь напоминание текстом.";
	private static final String UNSUPPORTED_MESSAGE =
			"Пока я принимаю только текстовые напоминания. "
					+ "Напиши, например: «завтра в 15:00 покормить кота».";

	private final ReminderTextInterpreter reminderTextInterpreter;
	private final ReminderDraftService reminderDraftService;
	private final ReminderService reminderService;
	private final ReminderCancellationService reminderCancellationService;
	private final ReminderSnoozeService reminderSnoozeService;
	private final TelegramClient telegramClient;
	private final SpeechTranscriber speechTranscriber;

	public void handle(TelegramUpdate update) {
		if (update.message() != null) {
			handleMessage(update.message());
			return;
		}
		if (isConfirmationCallback(update.callbackQuery())) {
			handleConfirmation(update.callbackQuery());
			return;
		}
		var reminderAction = reminderAction(update.callbackQuery());
		if (reminderAction.isPresent()) {
			handleReminderAction(update.callbackQuery(), reminderAction.orElseThrow());
			return;
		}
		if (update.callbackQuery() != null) {
			telegramClient.answerCallbackQuery(update.callbackQuery().id());
			return;
		}

		log.debug("Ignoring unsupported Telegram update: {}", update.updateId());
	}

	private void handleMessage(TelegramUpdate.TelegramMessage message) {
		if (message.text() != null) {
			handleTextMessage(message);
			return;
		}
		if (message.voice() != null) {
			handleVoiceMessage(message);
			return;
		}

		telegramClient.sendMessage(message.chat().id(), UNSUPPORTED_MESSAGE);
	}

	private boolean isConfirmationCallback(TelegramUpdate.TelegramCallbackQuery callbackQuery) {
		return callbackQuery != null
				&& callbackQuery.data() != null
				&& callbackQuery.data().startsWith(CONFIRM_CALLBACK_PREFIX);
	}

	private Optional<TelegramReminderAction> reminderAction(
			TelegramUpdate.TelegramCallbackQuery callbackQuery) {
		if (callbackQuery == null) {
			return Optional.empty();
		}
		return TelegramReminderAction.parse(callbackQuery.data());
	}

	private void handleTextMessage(TelegramUpdate.TelegramMessage message) {
		if (isCommand(message.text(), START_COMMAND)) {
			telegramClient.sendMessage(message.chat().id(), START_MESSAGE);
			return;
		}
		if (isCommand(message.text(), HELP_COMMAND)) {
			telegramClient.sendMessage(message.chat().id(), HELP_MESSAGE);
			return;
		}

		createDraftPreview(message.from().id(), message.chat().id(), message.text());
	}

	private void handleVoiceMessage(TelegramUpdate.TelegramMessage message) {
		if (!speechTranscriber.isAvailable()) {
			telegramClient.sendMessage(message.chat().id(), VOICE_DISABLED_MESSAGE);
			return;
		}

		var audio = telegramClient.downloadFile(message.voice().fileId());
		if (audio == null || audio.length == 0) {
			throw new IllegalStateException("Downloaded voice message is empty");
		}

		var transcription = speechTranscriber.transcribe(audio);
		if (transcription == null || transcription.isBlank()) {
			throw new IllegalStateException("Speech transcription is empty");
		}

		createDraftPreview(message.from().id(), message.chat().id(), transcription.strip());
	}

	private boolean isCommand(String text, String command) {
		var normalizedText = text.strip();
		return normalizedText.equals(command) || normalizedText.startsWith(command + "@");
	}

	private void createDraftPreview(long telegramUserId, long chatId, String text) {
		var result = reminderTextInterpreter.interpret(text);
		if (result instanceof ReminderClarification clarification) {
			telegramClient.sendMessage(chatId, clarification.question());
			return;
		}

		var interpretation = (ReminderInterpretation) result;
		var draft = reminderDraftService.create(telegramUserId, interpretation);
		telegramClient.sendConfirmation(
				chatId,
				formatPreview(interpretation),
				CONFIRM_CALLBACK_PREFIX + draft.getId());
	}

	private void handleConfirmation(TelegramUpdate.TelegramCallbackQuery callbackQuery) {
		var draftId = UUID.fromString(callbackQuery.data().substring(CONFIRM_CALLBACK_PREFIX.length()));
		var confirmation = reminderDraftService.confirm(draftId, callbackQuery.from().id());
		telegramClient.sendMessage(
				callbackQuery.message().chat().id(),
				formatConfirmation(confirmation));
		telegramClient.answerCallbackQuery(callbackQuery.id());
	}

	private void handleReminderAction(
			TelegramUpdate.TelegramCallbackQuery callbackQuery,
			TelegramReminderAction action) {
		switch (action.type()) {
			case COMPLETE -> handleCompletion(callbackQuery, action.targetId());
			case SNOOZE -> handleSnooze(callbackQuery, action.targetId());
			case CANCEL -> handleCancellation(callbackQuery, action.targetId());
		}
		telegramClient.answerCallbackQuery(callbackQuery.id());
	}

	private void handleCompletion(TelegramUpdate.TelegramCallbackQuery callbackQuery, UUID reminderId) {
		var reminder = reminderService.complete(reminderId, callbackQuery.from().id());
		telegramClient.sendMessage(
				callbackQuery.message().chat().id(),
				"Напоминание выполнено: " + reminder.getText());
	}

	private void handleSnooze(TelegramUpdate.TelegramCallbackQuery callbackQuery, UUID deliveryId) {
		var result = reminderSnoozeService.snooze(deliveryId, callbackQuery.from().id(), SNOOZE_DELAY);
		telegramClient.sendMessage(
				callbackQuery.message().chat().id(),
				"Напомню через час: " + result.text());
	}

	private void handleCancellation(TelegramUpdate.TelegramCallbackQuery callbackQuery, UUID reminderId) {
		var reminder = reminderCancellationService.cancel(reminderId, callbackQuery.from().id());
		telegramClient.sendMessage(
				callbackQuery.message().chat().id(),
				"Напоминание отменено: " + reminder.getText());
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
