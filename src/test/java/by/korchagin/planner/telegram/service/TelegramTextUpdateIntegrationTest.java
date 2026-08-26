package by.korchagin.planner.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.reminder.dto.ReminderInterpretation;
import by.korchagin.planner.reminder.repository.ReminderRepository;
import by.korchagin.planner.reminder.service.ReminderTextInterpreter;
import by.korchagin.planner.telegram.client.TelegramClient;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional(transactionManager = "transactionManager")
class TelegramTextUpdateIntegrationTest {

	private static final long TELEGRAM_USER_ID = 42L;
	private static final Instant REMIND_AT = Instant.parse("2026-08-14T12:00:00Z");
	private static final ZoneId USER_TIME_ZONE = ZoneId.of("Europe/Moscow");

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private TelegramUpdateService telegramUpdateService;

	@Autowired
	private ReminderRepository reminderRepository;

	@MockitoBean
	private ReminderTextInterpreter reminderTextInterpreter;

	@MockitoBean
	private TelegramClient telegramClient;

	@Test
	void handle_whenTextContainsReminder_shouldSendPreviewWithoutScheduling() throws IOException {
		var update = readUpdateFixture();
		when(reminderTextInterpreter.interpret("Завтра в 15:00 покормить кота"))
				.thenReturn(new ReminderInterpretation("Покормить кота", REMIND_AT, USER_TIME_ZONE));

		telegramUpdateService.handle(update);

		verify(reminderTextInterpreter).interpret("Завтра в 15:00 покормить кота");
		verify(telegramClient).sendConfirmation(
				eq(TELEGRAM_USER_ID),
				eq("Проверь напоминание:\n14.08.2026, 15:00 — Покормить кота"),
				argThat(actions -> actions.confirmationData().startsWith("reminder:confirm:")));
		assertThat(reminderRepository.count()).isZero();
	}

	private TelegramUpdate readUpdateFixture() throws IOException {
		try (var inputStream = new ClassPathResource("telegram/text-reminder-update.json").getInputStream()) {
			return jsonMapper.readValue(inputStream, TelegramUpdate.class);
		}
	}
}
