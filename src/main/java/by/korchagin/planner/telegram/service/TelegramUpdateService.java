package by.korchagin.planner.telegram.service;

import java.time.Clock;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import by.korchagin.planner.reminder.dto.ReminderConfirmation;
import by.korchagin.planner.reminder.dto.ReminderClarification;
import by.korchagin.planner.reminder.dto.ReminderInterpretation;
import by.korchagin.planner.reminder.entity.Reminder;
import by.korchagin.planner.reminder.exception.ReminderDraftNotFoundException;
import by.korchagin.planner.reminder.service.ReminderCancellationService;
import by.korchagin.planner.reminder.service.ReminderConversationService;
import by.korchagin.planner.reminder.service.ReminderDraftService;
import by.korchagin.planner.reminder.service.ReminderService;
import by.korchagin.planner.reminder.service.ReminderSnoozeService;
import by.korchagin.planner.telegram.client.TelegramClient;
import by.korchagin.planner.telegram.dto.TelegramDraftActions;
import by.korchagin.planner.telegram.dto.TelegramReminderAction;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import by.korchagin.planner.telegram.repository.TelegramUpdateReceiptRepository;
import by.korchagin.planner.voice.service.SpeechTranscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUpdateService {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
	private static final String CONFIRM_CALLBACK_PREFIX = "reminder:confirm:";
	private static final String EDIT_DRAFT_CALLBACK_PREFIX = "reminder:draft:edit:";
	private static final String CANCEL_DRAFT_CALLBACK_PREFIX = "reminder:draft:cancel:";
	private static final Duration SNOOZE_DELAY = Duration.ofHours(1);
	private static final String START_COMMAND = "/start";
	private static final String HELP_COMMAND = "/help";
	private static final String CANCEL_COMMAND = "/cancel";
	private static final String START_MESSAGE = "Привет! Я помогу не забыть важное.\n"
			+ "Напиши, что и когда напомнить. Например: «завтра в 15:00 покормить кота».\n"
			+ "Если чего-то не хватит, я уточню.";
	private static final String HELP_MESSAGE = "Можно написать свободно:\n"
			+ "• «в пятницу в 18:30 купить корм коту»\n"
			+ "• «через 10 минут выключить духовку»\n"
			+ "Если даты или времени не хватает, я уточню. /cancel сбрасывает текущий диалог.";
	private static final String VOICE_DISABLED_MESSAGE =
			"Голосовые сообщения пока не поддерживаются. Отправь напоминание текстом.";
	private static final String CANCEL_MESSAGE = "Хорошо, текущее напоминание сброшено.";
	private static final String UNKNOWN_COMMAND_MESSAGE =
			"Не знаю такую команду. Используй /help или просто напиши, что и когда напомнить.";
	private static final String TOO_LONG_MESSAGE =
			"Сообщение слишком длинное. Сформулируй напоминание короче 500 символов.";
	private static final String UNSUPPORTED_MESSAGE =
			"Пока я принимаю только текстовые напоминания. "
					+ "Напиши, например: «завтра в 15:00 покормить кота».";

	private final ReminderConversationService reminderConversationService;
	private final ReminderDraftService reminderDraftService;
	private final ReminderService reminderService;
	private final ReminderCancellationService reminderCancellationService;
	private final ReminderSnoozeService reminderSnoozeService;
	private final TelegramClient telegramClient;
	private final SpeechTranscriber speechTranscriber;
	private final TelegramUpdateReceiptRepository telegramUpdateReceiptRepository;
	private final Clock clock;

	@Transactional(transactionManager = "transactionManager")
	public void handle(TelegramUpdate update) {
		if (telegramUpdateReceiptRepository.claim(update.updateId(), clock.instant()) == 0) {
			log.debug("Ignoring duplicate Telegram update: {}", update.updateId());
			return;
		}

		if (update.message() != null) {
			handleMessage(update.message());
			return;
		}
		if (isConfirmationCallback(update.callbackQuery())) {
			handleConfirmation(update.callbackQuery());
			return;
		}
		if (isDraftCallback(update.callbackQuery(), EDIT_DRAFT_CALLBACK_PREFIX)) {
			handleDraftEditing(update.callbackQuery());
			return;
		}
		if (isDraftCallback(update.callbackQuery(), CANCEL_DRAFT_CALLBACK_PREFIX)) {
			handleDraftCancellation(update.callbackQuery());
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

	private boolean isDraftCallback(
			TelegramUpdate.TelegramCallbackQuery callbackQuery,
			String prefix) {
		return callbackQuery != null
				&& callbackQuery.data() != null
				&& callbackQuery.data().startsWith(prefix);
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
			reminderConversationService.reset(message.from().id(), message.chat().id());
			telegramClient.sendMessage(message.chat().id(), START_MESSAGE);
			return;
		}
		if (isCommand(message.text(), HELP_COMMAND)) {
			telegramClient.sendMessage(message.chat().id(), HELP_MESSAGE);
			return;
		}
		if (isCommand(message.text(), CANCEL_COMMAND)) {
			reminderConversationService.reset(message.from().id(), message.chat().id());
			telegramClient.sendMessage(message.chat().id(), CANCEL_MESSAGE);
			return;
		}
		if (message.text().strip().startsWith("/")) {
			telegramClient.sendMessage(message.chat().id(), UNKNOWN_COMMAND_MESSAGE);
			return;
		}
		if (message.text().length() > Reminder.MAX_TEXT_LENGTH) {
			telegramClient.sendMessage(message.chat().id(), TOO_LONG_MESSAGE);
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
		var result = reminderConversationService.interpret(telegramUserId, chatId, text);
		if (result instanceof ReminderClarification clarification) {
			telegramClient.sendMessage(chatId, clarification.question());
			return;
		}

		var interpretation = (ReminderInterpretation) result;
		var draft = reminderDraftService.create(telegramUserId, interpretation);
		telegramClient.sendConfirmation(
				chatId,
				formatPreview(interpretation),
				new TelegramDraftActions(
						CONFIRM_CALLBACK_PREFIX + draft.getId(),
						EDIT_DRAFT_CALLBACK_PREFIX + draft.getId(),
						CANCEL_DRAFT_CALLBACK_PREFIX + draft.getId()));
	}

	private void handleConfirmation(TelegramUpdate.TelegramCallbackQuery callbackQuery) {
		var draftId = UUID.fromString(callbackQuery.data().substring(CONFIRM_CALLBACK_PREFIX.length()));
		try {
			var confirmation = reminderDraftService.confirm(draftId, callbackQuery.from().id());
			if (confirmation.created()) {
				telegramClient.sendMessage(
						callbackQuery.message().chat().id(),
						formatConfirmation(confirmation));
			}
		}
		catch (ReminderDraftNotFoundException exception) {
			telegramClient.sendMessage(
					callbackQuery.message().chat().id(),
					"Этот вариант уже неактуален. Отправь новое напоминание.");
		}
		telegramClient.answerCallbackQuery(callbackQuery.id());
	}

	private void handleDraftEditing(TelegramUpdate.TelegramCallbackQuery callbackQuery) {
		discardDraft(callbackQuery, EDIT_DRAFT_CALLBACK_PREFIX);
		reminderConversationService.reset(callbackQuery.from().id(), callbackQuery.message().chat().id());
		telegramClient.sendMessage(
				callbackQuery.message().chat().id(),
				"Отправь исправленное напоминание целиком — старый вариант я удалил.");
		telegramClient.answerCallbackQuery(callbackQuery.id());
	}

	private void handleDraftCancellation(TelegramUpdate.TelegramCallbackQuery callbackQuery) {
		discardDraft(callbackQuery, CANCEL_DRAFT_CALLBACK_PREFIX);
		telegramClient.sendMessage(callbackQuery.message().chat().id(), "Черновик напоминания отменён.");
		telegramClient.answerCallbackQuery(callbackQuery.id());
	}

	private void discardDraft(TelegramUpdate.TelegramCallbackQuery callbackQuery, String prefix) {
		var draftId = UUID.fromString(callbackQuery.data().substring(prefix.length()));
		try {
			reminderDraftService.discard(draftId, callbackQuery.from().id());
		}
		catch (ReminderDraftNotFoundException exception) {
			log.debug("Reminder draft is already inactive: {}", draftId);
		}
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
