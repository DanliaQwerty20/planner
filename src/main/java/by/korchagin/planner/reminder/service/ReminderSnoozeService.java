package by.korchagin.planner.reminder.service;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import by.korchagin.planner.reminder.dto.ReminderSnoozeResult;
import by.korchagin.planner.reminder.exception.InvalidReminderException;
import by.korchagin.planner.reminder.exception.ReminderDeliveryNotFoundException;
import by.korchagin.planner.reminder.repository.ReminderDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReminderSnoozeService {

	private final ReminderDeliveryRepository reminderDeliveryRepository;
	private final ReminderService reminderService;
	private final Clock clock;

	@Transactional(transactionManager = "transactionManager")
	public ReminderSnoozeResult snooze(UUID deliveryId, long telegramUserId, Duration delay) {
		validate(deliveryId, telegramUserId, delay);
		var delivery = reminderDeliveryRepository.findByIdAndTelegramUserId(deliveryId, telegramUserId)
				.orElseThrow(() -> new ReminderDeliveryNotFoundException(
						"Reminder delivery not found: " + deliveryId));

		if (delivery.getSnoozedUntil() == null) {
			var snoozedUntil = clock.instant().plus(delay);
			reminderService.reschedule(delivery.getReminderId(), telegramUserId, snoozedUntil);
			delivery.markSnoozedUntil(snoozedUntil);
		}

		return new ReminderSnoozeResult(delivery.getText(), delivery.getSnoozedUntil());
	}

	private void validate(UUID deliveryId, long telegramUserId, Duration delay) {
		if (deliveryId == null) {
			throw new InvalidReminderException("Reminder delivery id must not be null");
		}
		if (telegramUserId <= 0) {
			throw new InvalidReminderException("Telegram user id must be positive");
		}
		if (delay == null || delay.isZero() || delay.isNegative()) {
			throw new InvalidReminderException("Snooze delay must be positive");
		}
	}
}
