package by.korchagin.planner.reminder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.reminder.entity.ReminderDeliveryStatus;
import by.korchagin.planner.reminder.repository.ReminderDeliveryRepository;
import by.korchagin.planner.reminder.repository.ReminderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional(transactionManager = "transactionManager")
class ReminderDeliveryIntegrationTest {

	private static final long TELEGRAM_USER_ID = 42L;
	private static final Instant INITIAL_TIME = Instant.parse("2026-08-11T12:00:00Z");

	@MockitoBean
	private Clock clock;

	@Autowired
	private ReminderService reminderService;

	@Autowired
	private ReminderRepository reminderRepository;

	@Autowired
	private ReminderDeliveryService reminderDeliveryService;

	@Autowired
	private ReminderDeliveryRepository reminderDeliveryRepository;

	@Test
	void enqueueDueReminders_whenCalledRepeatedly_shouldCreateOnePendingDeliveryPerDueReminder() {
		var currentTime = new AtomicReference<>(INITIAL_TIME);
		when(clock.instant()).thenAnswer(ignored -> currentTime.get());

		var dueReminder = reminderService.create(
				TELEGRAM_USER_ID,
				"Покормить кота",
				INITIAL_TIME.plusSeconds(60));
		var completedReminder = reminderService.create(
				TELEGRAM_USER_ID,
				"Уже выполнено",
				INITIAL_TIME.plusSeconds(60));
		reminderService.create(
				TELEGRAM_USER_ID,
				"Позвонить вечером",
				INITIAL_TIME.plusSeconds(7200));
		reminderService.complete(completedReminder.getId(), TELEGRAM_USER_ID);
		reminderRepository.flush();
		currentTime.set(INITIAL_TIME.plusSeconds(120));

		var firstEnqueueCount = reminderDeliveryService.enqueueDueReminders();
		var secondEnqueueCount = reminderDeliveryService.enqueueDueReminders();
		var deliveries = reminderDeliveryRepository.findAll();

		assertThat(firstEnqueueCount).isOne();
		assertThat(secondEnqueueCount).isZero();
		assertThat(deliveries).singleElement().satisfies(delivery -> {
			assertThat(delivery.getReminderId()).isEqualTo(dueReminder.getId());
			assertThat(delivery.getTelegramUserId()).isEqualTo(TELEGRAM_USER_ID);
			assertThat(delivery.getText()).isEqualTo("Покормить кота");
			assertThat(delivery.getStatus()).isEqualTo(ReminderDeliveryStatus.PENDING);
			assertThat(delivery.getCreatedAt()).isEqualTo(currentTime.get());
		});
	}
}
