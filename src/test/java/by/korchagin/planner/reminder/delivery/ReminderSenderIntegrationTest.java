package by.korchagin.planner.reminder.delivery;

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
import by.korchagin.planner.reminder.delivery.entity.ReminderDeliveryStatus;
import by.korchagin.planner.reminder.delivery.gateway.ReminderMessageSender;
import by.korchagin.planner.reminder.delivery.repository.ReminderDeliveryRepository;
import by.korchagin.planner.reminder.delivery.service.ReminderDeliveryService;
import by.korchagin.planner.reminder.delivery.service.ReminderDeliverySenderService;
import by.korchagin.planner.reminder.repository.ReminderRepository;
import by.korchagin.planner.reminder.service.ReminderService;
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
	private ReminderMessageSender reminderMessageSender;

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
		var reminderId = createPendingDelivery();

		var sent = reminderDeliverySenderService.sendNextPendingDelivery();
		var delivery = reminderDeliveryRepository.findById(reminderId).orElseThrow();

		assertThat(sent).isTrue();
		verify(reminderMessageSender).send(TELEGRAM_USER_ID, "Покормить кота");
		assertThat(delivery.getStatus()).isEqualTo(ReminderDeliveryStatus.SENT);
		assertThat(delivery.getSentAt()).isEqualTo(PROCESSING_TIME);

		assertThat(reminderDeliverySenderService.sendNextPendingDelivery()).isFalse();
		verifyNoMoreInteractions(reminderMessageSender);
	}

	@Test
	void sendNextPendingDelivery_whenTelegramFails_shouldKeepDeliveryPendingForRetry() {
		var reminderId = createPendingDelivery();
		doThrow(new RuntimeException("Telegram unavailable"))
				.when(reminderMessageSender)
				.send(TELEGRAM_USER_ID, "Покормить кота");

		assertThatThrownBy(reminderDeliverySenderService::sendNextPendingDelivery)
				.isInstanceOf(RuntimeException.class)
				.hasMessage("Telegram unavailable");

		var delivery = reminderDeliveryRepository.findById(reminderId).orElseThrow();
		assertThat(delivery.getStatus()).isEqualTo(ReminderDeliveryStatus.PENDING);
		assertThat(delivery.getSentAt()).isNull();
	}

	private UUID createPendingDelivery() {
		var reminder = reminderService.create(
				TELEGRAM_USER_ID,
				"Покормить кота",
				CREATION_TIME.plusSeconds(60));
		when(clock.instant()).thenReturn(PROCESSING_TIME);
		reminderDeliveryService.enqueueDueReminders();
		return reminder.getId();
	}
}
