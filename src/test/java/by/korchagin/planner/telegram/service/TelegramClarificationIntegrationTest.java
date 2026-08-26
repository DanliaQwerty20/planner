package by.korchagin.planner.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
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

	@Test
	void handle_whenUserAnswersClarification_shouldCompleteOriginalReminder() {
		telegramUpdateService.handle(textUpdate(
				10005L,
				"Завтра встретиться с дядей"));
		telegramUpdateService.handle(textUpdate(
				10006L,
				"примерно в 14 00"));

		verify(telegramClient).sendMessage(TELEGRAM_USER_ID, "Во сколько напомнить?");
		verify(telegramClient).sendConfirmation(
				eq(TELEGRAM_USER_ID),
				argThat(message -> message.endsWith("— Встретиться с дядей")),
				argThat(actions -> actions.confirmationData().startsWith("reminder:confirm:")));
		assertThat(reminderDraftRepository.findAll())
				.singleElement()
				.satisfies(draft -> assertThat(draft.getText()).isEqualTo("Встретиться с дядей"));
	}

	private TelegramUpdate textUpdate(long updateId, String text) {
		return new TelegramUpdate(
				updateId,
				new TelegramUpdate.TelegramMessage(
						updateId,
						new TelegramUpdate.TelegramUser(TELEGRAM_USER_ID),
						new TelegramUpdate.TelegramChat(TELEGRAM_USER_ID),
						text,
						null),
				null);
	}
}
