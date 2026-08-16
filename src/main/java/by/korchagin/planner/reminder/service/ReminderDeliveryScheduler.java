package by.korchagin.planner.reminder.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
		prefix = "planner.reminder.scheduling",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class ReminderDeliveryScheduler {

	private final ReminderDeliveryService reminderDeliveryService;
	private final ReminderDeliverySenderService reminderDeliverySenderService;

	@Scheduled(
			initialDelayString = "${planner.reminder.scheduling.initial-delay:5s}",
			fixedDelayString = "${planner.reminder.scheduling.enqueue-interval:1s}")
	public void enqueueDueReminders() {
		reminderDeliveryService.enqueueDueReminders();
	}

	@Scheduled(
			initialDelayString = "${planner.reminder.scheduling.initial-delay:5s}",
			fixedDelayString = "${planner.reminder.scheduling.send-interval:1s}")
	public void sendNextPendingDelivery() {
		reminderDeliverySenderService.sendNextPendingDelivery();
	}
}
