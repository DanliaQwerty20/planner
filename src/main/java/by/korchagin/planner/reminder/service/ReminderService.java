package by.korchagin.planner.reminder.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import by.korchagin.planner.reminder.entity.Reminder;
import by.korchagin.planner.reminder.exception.InvalidReminderException;
import by.korchagin.planner.reminder.exception.ReminderNotFoundException;
import by.korchagin.planner.reminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager")
public class ReminderService {

	private final ReminderRepository reminderRepository;
	private final Clock clock;

	public Reminder create(long telegramUserId, String text, Instant remindAt) {
		var now = clock.instant();
		var reminder = Reminder.schedule(telegramUserId, text, remindAt, now);
		return reminderRepository.save(reminder);
	}

	public Reminder complete(UUID id, long telegramUserId) {
		var reminder = findByIdAndTelegramUserId(id, telegramUserId);
		reminder.complete(clock.instant());
		return reminder;
	}

	public Reminder reschedule(UUID id, long telegramUserId, Instant newRemindAt) {
		var reminder = findByIdAndTelegramUserId(id, telegramUserId);
		reminder.reschedule(newRemindAt, clock.instant());
		return reminder;
	}

	private Reminder findByIdAndTelegramUserId(UUID id, long telegramUserId) {
		if (id == null) {
			throw new InvalidReminderException("Reminder id must not be null");
		}
		if (telegramUserId <= 0) {
			throw new InvalidReminderException("Telegram user id must be positive");
		}

		return reminderRepository.findByIdAndTelegramUserId(id, telegramUserId)
				.orElseThrow(() -> new ReminderNotFoundException("Reminder not found: " + id));
	}
}
