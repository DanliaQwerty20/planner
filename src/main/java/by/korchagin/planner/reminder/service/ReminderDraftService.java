package by.korchagin.planner.reminder.service;

import java.time.Clock;
import java.util.UUID;

import by.korchagin.planner.reminder.dto.ReminderConfirmation;
import by.korchagin.planner.reminder.dto.ReminderInterpretation;
import by.korchagin.planner.reminder.entity.ReminderDraft;
import by.korchagin.planner.reminder.exception.ReminderDraftNotFoundException;
import by.korchagin.planner.reminder.repository.ReminderDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager")
public class ReminderDraftService {

	private final ReminderDraftRepository reminderDraftRepository;
	private final ReminderService reminderService;
	private final Clock clock;

	public ReminderDraft create(long telegramUserId, ReminderInterpretation interpretation) {
		var draft = ReminderDraft.create(
				telegramUserId,
				interpretation.text(),
				interpretation.remindAt(),
				interpretation.timeZone());
		return reminderDraftRepository.save(draft);
	}

	public ReminderConfirmation confirm(UUID draftId, long telegramUserId) {
		var draft = reminderDraftRepository.findByIdAndTelegramUserId(draftId, telegramUserId)
				.orElseThrow(() -> new ReminderDraftNotFoundException("Reminder draft not found: " + draftId));

		if (!draft.isConfirmed()) {
			var reminder = reminderService.create(
					telegramUserId,
					draft.getText(),
					draft.getRemindAt());
			draft.confirm(reminder.getId(), clock.instant());
		}

		return new ReminderConfirmation(
				draft.getReminderId(),
				draft.getText(),
				draft.getRemindAt(),
				draft.getTimeZone());
	}
}
