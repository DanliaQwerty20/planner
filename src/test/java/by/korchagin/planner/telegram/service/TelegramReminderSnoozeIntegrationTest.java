package by.korchagin.planner.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import by.korchagin.planner.reminder.exception.ReminderDeliveryNotFoundException;
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
class TelegramReminderSnoozeIntegrationTest {

	private static final long TELEGRAM_USER_ID = 42L;
	private static final Instant INITIAL_TIME = Instant.parse("2026-08-15T09:00:00Z");

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
	void handle_whenSnoozeCallbackIsRepeated_shouldRescheduleAndDeliverReminderOnceMore() {
		var sentReminder = createSentReminder();
		telegramUpdateService.handle(snoozeCallback(101L, sentReminder.deliveryId(), TELEGRAM_USER_ID));
		telegramUpdateService.handle(snoozeCallback(102L, sentReminder.deliveryId(), TELEGRAM_USER_ID));

		var snoozedUntil = currentTime.get().plusSeconds(3600);
		var snoozedReminder = reminderRepository.findById(sentReminder.reminderId()).orElseThrow();
		assertThat(snoozedReminder.getStatus()).isEqualTo(ReminderStatus.SCHEDULED);
		assertThat(snoozedReminder.getRemindAt()).isEqualTo(snoozedUntil);
		assertThat(reminderDeliveryRepository.count()).isOne();
		assertThat(reminderDeliveryRepository.findById(sentReminder.deliveryId()).orElseThrow().getSnoozedUntil())
				.isEqualTo(snoozedUntil);
		verify(telegramClient, times(2)).sendMessage(
				TELEGRAM_USER_ID,
				"Напомню через час: Покормить кота");

		currentTime.set(snoozedUntil);
		assertThat(reminderDeliveryService.enqueueDueReminders()).isOne();
		assertThat(reminderDeliverySenderService.sendNextPendingDelivery()).isTrue();
		assertThat(reminderDeliveryRepository.findAll())
				.hasSize(2)
				.allSatisfy(delivery -> assertThat(delivery.getStatus())
						.isEqualTo(ReminderDeliveryStatus.SENT));
	}

	@Test
	void handle_whenSnoozeCallbackBelongsToAnotherUser_shouldRejectIt() {
		var sentReminder = createSentReminder();
		var callback = snoozeCallback(103L, sentReminder.deliveryId(), 99L);

		assertThatThrownBy(() -> telegramUpdateService.handle(callback))
				.isInstanceOf(ReminderDeliveryNotFoundException.class);

		var unchangedReminder = reminderRepository.findById(sentReminder.reminderId()).orElseThrow();
		assertThat(unchangedReminder.getRemindAt()).isEqualTo(sentReminder.originalRemindAt());
		assertThat(reminderDeliveryRepository.findById(sentReminder.deliveryId()).orElseThrow().getSnoozedUntil())
				.isNull();
	}

	private SentReminder createSentReminder() {
		var originalRemindAt = INITIAL_TIME.plusSeconds(60);
		var reminder = reminderService.create(
				TELEGRAM_USER_ID,
				"Покормить кота",
				originalRemindAt);
		currentTime.set(INITIAL_TIME.plusSeconds(120));
		reminderDeliveryService.enqueueDueReminders();
		reminderDeliverySenderService.sendNextPendingDelivery();
		var delivery = reminderDeliveryRepository.findAll().getFirst();
		return new SentReminder(reminder.getId(), delivery.getId(), originalRemindAt);
	}

	private TelegramUpdate snoozeCallback(long updateId, UUID deliveryId, long telegramUserId) {
		var user = new TelegramUpdate.TelegramUser(telegramUserId);
		var message = new TelegramUpdate.TelegramMessage(
				10L,
				user,
				new TelegramUpdate.TelegramChat(telegramUserId),
				null,
				null);
		var callbackQuery = new TelegramUpdate.TelegramCallbackQuery(
				"callback-snooze",
				user,
				message,
				"reminder:snooze:" + deliveryId);
		return new TelegramUpdate(updateId, null, callbackQuery);
	}

	private record SentReminder(UUID reminderId, UUID deliveryId, Instant originalRemindAt) {
	}
}
