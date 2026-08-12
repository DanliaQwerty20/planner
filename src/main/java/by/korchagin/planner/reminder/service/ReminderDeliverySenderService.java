package by.korchagin.planner.reminder.service;

import java.time.Clock;

import by.korchagin.planner.reminder.entity.ReminderDeliveryStatus;
import by.korchagin.planner.reminder.repository.ReminderDeliveryRepository;
import by.korchagin.planner.telegram.client.TelegramClient;
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

		telegramClient.sendMessage(delivery.getTelegramUserId(), delivery.getText());
		delivery.markSent(clock.instant());
		return true;
	}
}
