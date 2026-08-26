package by.korchagin.planner.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.reminder.dto.ReminderInterpretation;
import by.korchagin.planner.reminder.repository.ReminderDraftRepository;
import by.korchagin.planner.reminder.repository.ReminderRepository;
import by.korchagin.planner.reminder.service.ReminderTextInterpreter;
import by.korchagin.planner.telegram.client.TelegramClient;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import by.korchagin.planner.voice.service.SpeechTranscriber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional(transactionManager = "transactionManager")
class TelegramVoiceUpdateIntegrationTest {

	private static final long TELEGRAM_USER_ID = 42L;
	private static final Instant REMIND_AT = Instant.parse("2026-08-14T12:00:00Z");
	private static final ZoneId USER_TIME_ZONE = ZoneId.of("Europe/Moscow");
	private static final byte[] VOICE_AUDIO = "ogg-opus-audio".getBytes(StandardCharsets.UTF_8);

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private TelegramUpdateService telegramUpdateService;

	@Autowired
	private ReminderDraftRepository reminderDraftRepository;

	@Autowired
	private ReminderRepository reminderRepository;

	@MockitoBean
	private TelegramClient telegramClient;

	@MockitoBean
	private SpeechTranscriber speechTranscriber;

	@MockitoBean
	private ReminderTextInterpreter reminderTextInterpreter;

	@Test
	void handle_whenUpdateContainsVoice_shouldTranscribeAndCreateDraftPreview() throws IOException {
		when(speechTranscriber.isAvailable()).thenReturn(true);
		when(telegramClient.downloadFile("voice-file-1")).thenReturn(VOICE_AUDIO);
		when(speechTranscriber.transcribe(VOICE_AUDIO)).thenReturn("Завтра в 15:00 покормить кота");
		when(reminderTextInterpreter.interpret("Завтра в 15:00 покормить кота"))
				.thenReturn(new ReminderInterpretation("Покормить кота", REMIND_AT, USER_TIME_ZONE));

		telegramUpdateService.handle(readVoiceUpdate());

		verify(telegramClient).downloadFile("voice-file-1");
		verify(speechTranscriber).transcribe(VOICE_AUDIO);
		verify(reminderTextInterpreter).interpret("Завтра в 15:00 покормить кота");
		verify(telegramClient).sendConfirmation(
				eq(TELEGRAM_USER_ID),
				eq("Проверь напоминание:\n14.08.2026, 15:00 — Покормить кота"),
				argThat(actions -> actions.confirmationData().startsWith("reminder:confirm:")));
		assertThat(reminderDraftRepository.findAll()).singleElement().satisfies(draft -> {
			assertThat(draft.getTelegramUserId()).isEqualTo(TELEGRAM_USER_ID);
			assertThat(draft.getText()).isEqualTo("Покормить кота");
			assertThat(draft.getRemindAt()).isEqualTo(REMIND_AT);
		});
		assertThat(reminderRepository.count()).isZero();
	}

	@Test
	void handle_whenTranscriptionIsBlank_shouldRejectVoiceWithoutCreatingDraft() throws IOException {
		when(speechTranscriber.isAvailable()).thenReturn(true);
		when(telegramClient.downloadFile("voice-file-1")).thenReturn(VOICE_AUDIO);
		when(speechTranscriber.transcribe(VOICE_AUDIO)).thenReturn(" ");

		assertThatThrownBy(() -> telegramUpdateService.handle(readVoiceUpdate()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Speech transcription is empty");

		verifyNoInteractions(reminderTextInterpreter);
		assertThat(reminderDraftRepository.count()).isZero();
	}

	private TelegramUpdate readVoiceUpdate() throws IOException {
		try (var inputStream = new ClassPathResource("telegram/voice-reminder-update.json").getInputStream()) {
			return jsonMapper.readValue(inputStream, TelegramUpdate.class);
		}
	}
}
