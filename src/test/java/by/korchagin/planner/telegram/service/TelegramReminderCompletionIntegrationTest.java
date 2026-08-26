package by.korchagin.planner.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.reminder.entity.ReminderStatus;
import by.korchagin.planner.reminder.repository.ReminderDeliveryRepository;
import by.korchagin.planner.reminder.repository.ReminderRepository;
import by.korchagin.planner.reminder.service.ReminderService;
import by.korchagin.planner.telegram.client.TelegramClient;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import by.korchagin.planner.telegram.repository.TelegramUpdateReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TelegramReminderCompletionIntegrationTest {

	private static final long TELEGRAM_USER_ID = 42L;
	private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

	@Autowired
	private TelegramUpdateService telegramUpdateService;

	@Autowired
	private ReminderService reminderService;

	@Autowired
	private ReminderDeliveryRepository reminderDeliveryRepository;

	@Autowired
	private ReminderRepository reminderRepository;

	@Autowired
	private TelegramUpdateReceiptRepository telegramUpdateReceiptRepository;

	@MockitoBean
	private Clock clock;

	@MockitoBean
	private TelegramClient telegramClient;

	@BeforeEach
	void setUp() {
		telegramUpdateReceiptRepository.deleteAll();
		reminderDeliveryRepository.deleteAll();
		reminderRepository.deleteAll();
		when(clock.instant()).thenReturn(NOW);
	}

	@Test
	void handle_whenCompletionCallbackIsRepeated_shouldCompleteReminderIdempotently() {
		var reminder = reminderService.create(
				TELEGRAM_USER_ID,
				"Покормить кота",
				NOW.plusSeconds(60));
		telegramUpdateService.handle(completionCallback(100L, reminder.getId().toString()));
		telegramUpdateService.handle(completionCallback(101L, reminder.getId().toString()));

		var completedReminder = reminderRepository.findById(reminder.getId()).orElseThrow();
		assertThat(completedReminder.getStatus()).isEqualTo(ReminderStatus.COMPLETED);
		assertThat(completedReminder.getCompletedAt()).isEqualTo(NOW);
		verify(telegramClient, times(2)).sendMessage(
				TELEGRAM_USER_ID,
				"Напоминание выполнено: Покормить кота");
		verify(telegramClient, times(2)).answerCallbackQuery("callback-1");
	}

	private TelegramUpdate completionCallback(long updateId, String reminderId) {
		var user = new TelegramUpdate.TelegramUser(TELEGRAM_USER_ID);
		var message = new TelegramUpdate.TelegramMessage(
				10L,
				user,
				new TelegramUpdate.TelegramChat(TELEGRAM_USER_ID),
				null,
				null);
		var callbackQuery = new TelegramUpdate.TelegramCallbackQuery(
				"callback-1",
				user,
				message,
				"reminder:complete:" + reminderId);
		return new TelegramUpdate(updateId, null, callbackQuery);
	}
}
