package by.korchagin.planner.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.reminder.dto.ReminderInterpretation;
import by.korchagin.planner.reminder.entity.ReminderStatus;
import by.korchagin.planner.reminder.repository.ReminderDraftRepository;
import by.korchagin.planner.reminder.repository.ReminderRepository;
import by.korchagin.planner.reminder.service.ReminderTextInterpreter;
import by.korchagin.planner.telegram.client.TelegramClient;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import by.korchagin.planner.telegram.repository.TelegramUpdateReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TelegramReminderConfirmationIntegrationTest {

	private static final long TELEGRAM_USER_ID = 42L;
	private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
	private static final Instant REMIND_AT = Instant.parse("2026-08-14T12:00:00Z");
	private static final ZoneId USER_TIME_ZONE = ZoneId.of("Europe/Moscow");

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private TelegramUpdateService telegramUpdateService;

	@Autowired
	private ReminderDraftRepository reminderDraftRepository;

	@Autowired
	private ReminderRepository reminderRepository;

	@Autowired
	private TelegramUpdateReceiptRepository telegramUpdateReceiptRepository;

	@MockitoBean
	private Clock clock;

	@MockitoBean
	private ReminderTextInterpreter reminderTextInterpreter;

	@MockitoBean
	private TelegramClient telegramClient;

	@BeforeEach
	void setUp() {
		telegramUpdateReceiptRepository.deleteAll();
		reminderDraftRepository.deleteAll();
		reminderRepository.deleteAll();
	}

	@Test
	void handle_whenDraftIsConfirmedRepeatedly_shouldScheduleOneReminder() throws IOException {
		when(clock.instant()).thenReturn(NOW);
		when(reminderTextInterpreter.interpret("Завтра в 15:00 покормить кота"))
				.thenReturn(new ReminderInterpretation("Покормить кота", REMIND_AT, USER_TIME_ZONE));

		telegramUpdateService.handle(readUpdate("telegram/text-reminder-update.json"));

		var drafts = reminderDraftRepository.findAll();
		assertThat(drafts).hasSize(1);
		var draft = drafts.getFirst();
		assertThat(reminderRepository.count()).isZero();
		verify(telegramClient).sendConfirmation(
				eq(TELEGRAM_USER_ID),
				startsWith("Проверь напоминание:"),
				eq("reminder:confirm:" + draft.getId()));

		var callbackUpdate = readCallbackUpdate(draft.getId().toString());
		telegramUpdateService.handle(callbackUpdate);
		telegramUpdateService.handle(new TelegramUpdate(
				callbackUpdate.updateId() + 1,
				callbackUpdate.message(),
				callbackUpdate.callbackQuery()));

		var reminders = reminderRepository.findAll();
		assertThat(reminders).hasSize(1);
		var reminder = reminders.getFirst();
		assertThat(reminder.getTelegramUserId()).isEqualTo(TELEGRAM_USER_ID);
		assertThat(reminder.getText()).isEqualTo("Покормить кота");
		assertThat(reminder.getRemindAt()).isEqualTo(REMIND_AT);
		assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.SCHEDULED);
		assertThat(reminderDraftRepository.findById(draft.getId()).orElseThrow().getReminderId())
				.isEqualTo(reminder.getId());
		verify(telegramClient, times(2)).sendMessage(
				TELEGRAM_USER_ID,
				"Напоминание создано: 14.08.2026, 15:00 — Покормить кота");
		verify(telegramClient, times(2)).answerCallbackQuery("callback-1");
	}

	private TelegramUpdate readUpdate(String path) throws IOException {
		try (var inputStream = new ClassPathResource(path).getInputStream()) {
			return jsonMapper.readValue(inputStream, TelegramUpdate.class);
		}
	}

	private TelegramUpdate readCallbackUpdate(String draftId) throws IOException {
		var resource = new ClassPathResource("telegram/confirmation-callback-update.json");
		try (var inputStream = resource.getInputStream()) {
			var json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
					.replace("DRAFT_ID", draftId);
			return jsonMapper.readValue(json, TelegramUpdate.class);
		}
	}
}
