package by.korchagin.planner.telegram.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.reminder.service.ReminderTextInterpreter;
import by.korchagin.planner.telegram.client.TelegramClient;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional(transactionManager = "transactionManager")
class TelegramMvpInteractionIntegrationTest {

	private static final long TELEGRAM_USER_ID = 42L;
	private static final long CHAT_ID = 42L;

	@Autowired
	private TelegramUpdateService telegramUpdateService;

	@MockitoBean
	private ReminderTextInterpreter reminderTextInterpreter;

	@MockitoBean
	private TelegramClient telegramClient;

	@Test
	void handle_whenStartCommandReceived_shouldSendWelcomeWithoutInterpretingReminder() {
		telegramUpdateService.handle(textUpdate("/start"));

		verify(telegramClient).sendMessage(
				CHAT_ID,
				"Привет! Я помогу не забыть важное.\n"
						+ "Отправь напоминание текстом, например: «завтра в 15:00 покормить кота».");
		verifyNoInteractions(reminderTextInterpreter);
	}

	@Test
	void handle_whenHelpCommandReceived_shouldExplainTextReminderFlow() {
		telegramUpdateService.handle(textUpdate("/help"));

		verify(telegramClient).sendMessage(
				CHAT_ID,
				"Напиши одним сообщением, что и когда напомнить.\n"
						+ "Например: «в пятницу в 18:30 купить корм коту».\n"
						+ "Перед созданием я покажу дату и текст для подтверждения.");
		verifyNoInteractions(reminderTextInterpreter);
	}

	@Test
	void handle_whenVoiceReceivedWhileTranscriptionDisabled_shouldSuggestTextWithoutDownloadingFile() {
		telegramUpdateService.handle(voiceUpdate());

		verify(telegramClient).sendMessage(
				CHAT_ID,
				"Голосовые сообщения пока не поддерживаются. Отправь напоминание текстом.");
		verify(telegramClient, never()).downloadFile(anyString());
		verifyNoInteractions(reminderTextInterpreter);
	}

	@Test
	void handle_whenUnsupportedMessageReceived_shouldSuggestTextWithoutFailure() {
		telegramUpdateService.handle(unsupportedMessageUpdate());

		verify(telegramClient).sendMessage(
				CHAT_ID,
				"Пока я принимаю только текстовые напоминания. Напиши, например: «завтра в 15:00 покормить кота».");
		verifyNoInteractions(reminderTextInterpreter);
	}

	@Test
	void handle_whenUnknownCallbackReceived_shouldAcknowledgeWithoutFailure() {
		var update = callbackUpdate("unknown:action");

		assertThatNoException().isThrownBy(() -> telegramUpdateService.handle(update));

		verify(telegramClient).answerCallbackQuery("callback-1");
		verifyNoInteractions(reminderTextInterpreter);
	}

	@Test
	void handle_whenUpdateHasNoSupportedPayload_shouldIgnoreWithoutFailure() {
		var update = new TelegramUpdate(100L, null, null);

		assertThatNoException().isThrownBy(() -> telegramUpdateService.handle(update));

		verifyNoInteractions(telegramClient, reminderTextInterpreter);
	}

	private TelegramUpdate textUpdate(String text) {
		return new TelegramUpdate(100L, message(text, null), null);
	}

	private TelegramUpdate voiceUpdate() {
		var voice = new TelegramUpdate.TelegramVoice(
				"voice-file-id",
				"voice-file-unique-id",
				5,
				"audio/ogg",
				1024L);
		return new TelegramUpdate(100L, message(null, voice), null);
	}

	private TelegramUpdate unsupportedMessageUpdate() {
		return new TelegramUpdate(100L, message(null, null), null);
	}

	private TelegramUpdate callbackUpdate(String data) {
		var callback = new TelegramUpdate.TelegramCallbackQuery(
				"callback-1",
				new TelegramUpdate.TelegramUser(TELEGRAM_USER_ID),
				message(null, null),
				data);
		return new TelegramUpdate(100L, null, callback);
	}

	private TelegramUpdate.TelegramMessage message(
			String text,
			TelegramUpdate.TelegramVoice voice) {
		return new TelegramUpdate.TelegramMessage(
				10L,
				new TelegramUpdate.TelegramUser(TELEGRAM_USER_ID),
				new TelegramUpdate.TelegramChat(CHAT_ID),
				text,
				voice);
	}
}
