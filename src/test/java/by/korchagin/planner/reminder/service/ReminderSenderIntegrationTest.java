package by.korchagin.planner.reminder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.reminder.entity.ReminderDeliveryStatus;
import by.korchagin.planner.reminder.repository.ReminderDeliveryRepository;
import by.korchagin.planner.reminder.repository.ReminderRepository;
import by.korchagin.planner.telegram.client.TelegramClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReminderSenderIntegrationTest {

	private static final long TELEGRAM_USER_ID = 42L;
	private static final Instant CREATION_TIME = Instant.parse("2026-08-12T12:00:00Z");
	private static final Instant PROCESSING_TIME = CREATION_TIME.plusSeconds(120);

	@MockitoBean
	private Clock clock;

	@MockitoBean
	private TelegramClient telegramClient;

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

	@BeforeEach
	void setUp() {
		reminderDeliveryRepository.deleteAll();
		reminderRepository.deleteAll();
		when(clock.instant()).thenReturn(CREATION_TIME);
	}

	@Test
	void sendNextPendingDelivery_whenTelegramAcceptsMessage_shouldMarkDeliveryAsSent() {
		var pendingDelivery = createPendingDelivery();

		var sent = reminderDeliverySenderService.sendNextPendingDelivery();
		var delivery = reminderDeliveryRepository.findById(pendingDelivery.deliveryId()).orElseThrow();

		assertThat(sent).isTrue();
		verify(telegramClient).sendReminder(
				TELEGRAM_USER_ID,
				"Покормить кота",
				"reminder:complete:" + pendingDelivery.reminderId(),
				"reminder:snooze:" + pendingDelivery.deliveryId());
		assertThat(delivery.getStatus()).isEqualTo(ReminderDeliveryStatus.SENT);
		assertThat(delivery.getSentAt()).isEqualTo(PROCESSING_TIME);

		assertThat(reminderDeliverySenderService.sendNextPendingDelivery()).isFalse();
		verifyNoMoreInteractions(telegramClient);
	}

	@Test
	void sendNextPendingDelivery_whenTelegramFails_shouldKeepDeliveryPendingForRetry() {
		var pendingDelivery = createPendingDelivery();
		doThrow(new RuntimeException("Telegram unavailable"))
				.when(telegramClient)
				.sendReminder(
						TELEGRAM_USER_ID,
						"Покормить кота",
						"reminder:complete:" + pendingDelivery.reminderId(),
						"reminder:snooze:" + pendingDelivery.deliveryId());

		assertThatThrownBy(reminderDeliverySenderService::sendNextPendingDelivery)
				.isInstanceOf(RuntimeException.class)
				.hasMessage("Telegram unavailable");

		var delivery = reminderDeliveryRepository.findById(pendingDelivery.deliveryId()).orElseThrow();
		assertThat(delivery.getStatus()).isEqualTo(ReminderDeliveryStatus.PENDING);
		assertThat(delivery.getSentAt()).isNull();
	}

	private PendingDelivery createPendingDelivery() {
		var reminder = reminderService.create(
				TELEGRAM_USER_ID,
				"Покормить кота",
				CREATION_TIME.plusSeconds(60));
		when(clock.instant()).thenReturn(PROCESSING_TIME);
		reminderDeliveryService.enqueueDueReminders();
		var delivery = reminderDeliveryRepository.findAll().getFirst();
		return new PendingDelivery(delivery.getId(), reminder.getId());
	}

	private record PendingDelivery(UUID deliveryId, UUID reminderId) {
	}
}
