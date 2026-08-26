package by.korchagin.planner.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.reminder.dto.ReminderInterpretation;
import by.korchagin.planner.reminder.repository.ReminderDraftRepository;
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
class TelegramUpdateIdempotencyIntegrationTest {

	private static final long TELEGRAM_USER_ID = 42L;
	private static final Instant REMIND_AT = Instant.parse("2026-08-14T12:00:00Z");
	private static final ZoneId USER_TIME_ZONE = ZoneId.of("Europe/Moscow");
	private static final String MESSAGE_TEXT = "Завтра в 15:00 покормить кота";

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private TelegramUpdateService telegramUpdateService;

	@Autowired
	private ReminderDraftRepository reminderDraftRepository;

	@Autowired
	private TelegramUpdateReceiptRepository telegramUpdateReceiptRepository;

	@MockitoBean
	private ReminderTextInterpreter reminderTextInterpreter;

	@MockitoBean
	private TelegramClient telegramClient;

	@BeforeEach
	void setUp() {
		reminderDraftRepository.deleteAll();
		telegramUpdateReceiptRepository.deleteAll();
	}

	@Test
	void handle_whenSameUpdateIsReceivedTwice_shouldApplyItOnce() throws IOException {
		var update = readUpdateFixture();
		when(reminderTextInterpreter.interpret(MESSAGE_TEXT)).thenReturn(interpretation());

		telegramUpdateService.handle(update);
		telegramUpdateService.handle(update);

		assertThat(reminderDraftRepository.count()).isOne();
		assertThat(telegramUpdateReceiptRepository.count()).isOne();
		verify(telegramClient).sendConfirmation(
				eq(TELEGRAM_USER_ID),
				eq("Проверь напоминание:\n14.08.2026, 15:00 — Покормить кота"),
				startsWith("reminder:confirm:"));
	}

	@Test
	void handle_whenFirstAttemptFails_shouldRollbackReceiptAndAllowRetry() throws IOException {
		var update = readUpdateFixture();
		when(reminderTextInterpreter.interpret(MESSAGE_TEXT))
				.thenThrow(new IllegalStateException("Temporary interpretation failure"))
				.thenReturn(interpretation());

		assertThatThrownBy(() -> telegramUpdateService.handle(update))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Temporary interpretation failure");
		telegramUpdateService.handle(update);

		assertThat(reminderDraftRepository.count()).isOne();
		assertThat(telegramUpdateReceiptRepository.count()).isOne();
		verify(reminderTextInterpreter, times(2)).interpret(MESSAGE_TEXT);
	}

	private ReminderInterpretation interpretation() {
		return new ReminderInterpretation("Покормить кота", REMIND_AT, USER_TIME_ZONE);
	}

	private TelegramUpdate readUpdateFixture() throws IOException {
		try (var inputStream = new ClassPathResource("telegram/text-reminder-update.json").getInputStream()) {
			return jsonMapper.readValue(inputStream, TelegramUpdate.class);
		}
	}
}
