package by.korchagin.planner.reminder.service;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import by.korchagin.planner.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"planner.reminder.scheduling.enabled=true",
		"planner.reminder.scheduling.initial-delay=100ms",
		"planner.reminder.scheduling.enqueue-interval=50ms",
		"planner.reminder.scheduling.send-interval=50ms"
})
@Import(TestcontainersConfiguration.class)
class ReminderDeliverySchedulerIntegrationTest {

	@MockitoBean
	private ReminderDeliveryService reminderDeliveryService;

	@MockitoBean
	private ReminderDeliverySenderService reminderDeliverySenderService;

	@Test
	void scheduling_whenEnabled_shouldRunEnqueueAndSendWorkersPeriodically() {
		verify(reminderDeliveryService, timeout(5_000).atLeastOnce()).enqueueDueReminders();
		verify(reminderDeliverySenderService, timeout(5_000).atLeastOnce()).sendNextPendingDelivery();
	}
}
