package by.korchagin.planner.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.reminder.entity.ReminderDeliveryStatus;
import by.korchagin.planner.reminder.entity.ReminderStatus;
import by.korchagin.planner.reminder.repository.ReminderDeliveryRepository;
import by.korchagin.planner.reminder.repository.ReminderRepository;
import by.korchagin.planner.reminder.service.ReminderDeliverySenderService;
import by.korchagin.planner.reminder.service.ReminderDeliveryService;
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
class TelegramReminderCancellationIntegrationTest {

	private static final long TELEGRAM_USER_ID = 42L;
	private static final Instant INITIAL_TIME = Instant.parse("2026-08-16T09:00:00Z");

	@Autowired
	private TelegramUpdateService telegramUpdateService;

	@Autowired
	private ReminderService reminderService;

	@Autowired
	private ReminderDeliveryService reminderDeliveryService;

	@Autowired
	private ReminderDeliverySenderService reminderDeliverySenderService;

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

	private final AtomicReference<Instant> currentTime = new AtomicReference<>(INITIAL_TIME);

	@BeforeEach
	void setUp() {
		telegramUpdateReceiptRepository.deleteAll();
		reminderDeliveryRepository.deleteAll();
		reminderRepository.deleteAll();
		currentTime.set(INITIAL_TIME);
		when(clock.instant()).thenAnswer(ignored -> currentTime.get());
	}

	@Test
	void handle_whenCancellationCallbackIsRepeatedAfterSnooze_shouldCancelFutureDelivery() {
		var reminder = reminderService.create(
				TELEGRAM_USER_ID,
				"Покормить кота",
				INITIAL_TIME.plusSeconds(60));
		currentTime.set(INITIAL_TIME.plusSeconds(120));
		reminderDeliveryService.enqueueDueReminders();
		reminderDeliverySenderService.sendNextPendingDelivery();
		var firstDelivery = reminderDeliveryRepository.findAll().getFirst();
		telegramUpdateService.handle(reminderCallback(101L, "snooze", firstDelivery.getId()));
		var snoozedUntil = currentTime.get().plusSeconds(3600);
		currentTime.set(snoozedUntil);
		assertThat(reminderDeliveryService.enqueueDueReminders()).isOne();

		telegramUpdateService.handle(reminderCallback(102L, "cancel", reminder.getId()));
		telegramUpdateService.handle(reminderCallback(103L, "cancel", reminder.getId()));

		var cancelledReminder = reminderRepository.findById(reminder.getId()).orElseThrow();
		assertThat(cancelledReminder.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
		assertThat(cancelledReminder.getCancelledAt()).isEqualTo(currentTime.get());
		verify(telegramClient, times(2)).sendMessage(
				TELEGRAM_USER_ID,
				"Напоминание отменено: Покормить кота");

		assertThat(reminderDeliveryService.enqueueDueReminders()).isZero();
		assertThat(reminderDeliverySenderService.sendNextPendingDelivery()).isFalse();
		assertThat(reminderDeliveryRepository.findAll())
				.extracting(delivery -> delivery.getStatus())
				.containsExactlyInAnyOrder(ReminderDeliveryStatus.SENT, ReminderDeliveryStatus.CANCELLED);
	}

	private TelegramUpdate reminderCallback(long updateId, String action, UUID targetId) {
		var user = new TelegramUpdate.TelegramUser(TELEGRAM_USER_ID);
		var message = new TelegramUpdate.TelegramMessage(
				10L,
				user,
				new TelegramUpdate.TelegramChat(TELEGRAM_USER_ID),
				null,
				null);
		var callbackQuery = new TelegramUpdate.TelegramCallbackQuery(
				"callback-" + action,
				user,
				message,
				"reminder:" + action + ":" + targetId);
		return new TelegramUpdate(updateId, null, callbackQuery);
	}
}
