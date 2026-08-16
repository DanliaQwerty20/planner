package by.korchagin.planner.reminder.service;

import java.util.UUID;

import by.korchagin.planner.reminder.entity.Reminder;
import by.korchagin.planner.reminder.repository.ReminderDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderCancellationService {

	private final ReminderService reminderService;
	private final ReminderDeliveryRepository reminderDeliveryRepository;

	@Transactional(transactionManager = "transactionManager")
	public Reminder cancel(UUID reminderId, long telegramUserId) {
		var reminder = reminderService.cancel(reminderId, telegramUserId);
		reminderDeliveryRepository.cancelPendingByReminderId(reminderId);
		return reminder;
	}
}
