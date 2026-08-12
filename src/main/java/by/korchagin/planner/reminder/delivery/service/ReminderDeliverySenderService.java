package by.korchagin.planner.reminder.delivery.service;

import java.time.Clock;

import by.korchagin.planner.reminder.delivery.entity.ReminderDeliveryStatus;
import by.korchagin.planner.reminder.delivery.gateway.ReminderMessageSender;
import by.korchagin.planner.reminder.delivery.repository.ReminderDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderDeliverySenderService {

	private final ReminderDeliveryRepository reminderDeliveryRepository;
	private final ReminderMessageSender reminderMessageSender;
	private final Clock clock;

	@Transactional(transactionManager = "transactionManager")
	public boolean sendNextPendingDelivery() {
		var delivery = reminderDeliveryRepository
				.findFirstByStatusOrderByCreatedAtAsc(ReminderDeliveryStatus.PENDING)
				.orElse(null);
		if (delivery == null) {
			return false;
		}

		reminderMessageSender.send(delivery.getTelegramUserId(), delivery.getText());
		delivery.markSent(clock.instant());
		return true;
	}
}
