package by.korchagin.planner.reminder.delivery.service;

import java.time.Clock;

import by.korchagin.planner.reminder.delivery.repository.ReminderDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderDeliveryService {

	private final ReminderDeliveryRepository reminderDeliveryRepository;
	private final Clock clock;

	@Transactional(transactionManager = "transactionManager")
	public int enqueueDueReminders() {
		var currentTime = clock.instant();
		return reminderDeliveryRepository.enqueueDueReminders(currentTime, currentTime);
	}
}
