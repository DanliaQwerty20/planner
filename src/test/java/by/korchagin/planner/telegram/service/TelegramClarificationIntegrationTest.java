package by.korchagin.planner.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.reminder.repository.ReminderDraftRepository;
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
class TelegramClarificationIntegrationTest {

	private static final long TELEGRAM_USER_ID = 42L;

	@Autowired
	private TelegramUpdateService telegramUpdateService;

	@Autowired
	private ReminderDraftRepository reminderDraftRepository;

	@MockitoBean
	private TelegramClient telegramClient;

	@Test
	void handle_whenTextHasNoDay_shouldAskForDayWithoutCreatingDraft() {
		var update = new TelegramUpdate(
				10004L,
				new TelegramUpdate.TelegramMessage(
						54L,
						new TelegramUpdate.TelegramUser(TELEGRAM_USER_ID),
						new TelegramUpdate.TelegramChat(TELEGRAM_USER_ID),
						"В 15:00 покормить кота",
						null),
				null);

		telegramUpdateService.handle(update);

		verify(telegramClient).sendMessage(
				TELEGRAM_USER_ID,
				"На какой день поставить напоминание?");
		assertThat(reminderDraftRepository.count()).isZero();
	}
}
