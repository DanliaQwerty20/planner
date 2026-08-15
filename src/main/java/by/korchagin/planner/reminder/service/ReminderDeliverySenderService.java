package by.korchagin.planner.reminder.service;

import java.time.Clock;

import by.korchagin.planner.reminder.entity.ReminderDeliveryStatus;
import by.korchagin.planner.reminder.repository.ReminderDeliveryRepository;
import by.korchagin.planner.telegram.client.TelegramClient;
import by.korchagin.planner.telegram.dto.TelegramReminderAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderDeliverySenderService {

	private final ReminderDeliveryRepository reminderDeliveryRepository;
	private final TelegramClient telegramClient;
	private final Clock clock;

	@Transactional(transactionManager = "transactionManager")
	public boolean sendNextPendingDelivery() {
		var delivery = reminderDeliveryRepository
				.findFirstByStatusOrderByCreatedAtAsc(ReminderDeliveryStatus.PENDING)
				.orElse(null);
		if (delivery == null) {
			return false;
		}

		var completionAction = TelegramReminderAction.complete(delivery.getReminderId());
		var snoozeAction = TelegramReminderAction.snooze(delivery.getId());
		telegramClient.sendReminder(
				delivery.getTelegramUserId(),
				delivery.getText(),
				completionAction.data(),
				snoozeAction.data());
		delivery.markSent(clock.instant());
		return true;
	}
}
