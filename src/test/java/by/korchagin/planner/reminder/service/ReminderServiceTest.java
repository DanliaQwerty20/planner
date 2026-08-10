package by.korchagin.planner.reminder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import by.korchagin.planner.reminder.entity.Reminder;
import by.korchagin.planner.reminder.entity.ReminderStatus;
import by.korchagin.planner.reminder.exception.InvalidReminderException;
import by.korchagin.planner.reminder.exception.ReminderAlreadyCompletedException;
import by.korchagin.planner.reminder.exception.ReminderNotFoundException;
import by.korchagin.planner.reminder.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

	private static final long TELEGRAM_USER_ID = 42L;
	private static final UUID REMINDER_ID = UUID.fromString("7efdcce0-2f50-4ee7-97d0-ff32b10f736c");
	private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Mock
	private ReminderRepository reminderRepository;

	private ReminderService reminderService;

	@BeforeEach
	void setUp() {
		reminderService = new ReminderService(reminderRepository, CLOCK);
	}

	@Test
	void create_whenDataIsValid_shouldSaveAndReturnReminder() {
		var remindAt = NOW.plusSeconds(3600);
		when(reminderRepository.save(any(Reminder.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var result = reminderService.create(TELEGRAM_USER_ID, "  Покормить кота  ", remindAt);

		var captor = ArgumentCaptor.forClass(Reminder.class);
		verify(reminderRepository).save(captor.capture());
		assertThat(result).isSameAs(captor.getValue());
		assertThat(result.getTelegramUserId()).isEqualTo(TELEGRAM_USER_ID);
		assertThat(result.getText()).isEqualTo("Покормить кота");
		assertThat(result.getRemindAt()).isEqualTo(remindAt);
		assertThat(result.getStatus()).isEqualTo(ReminderStatus.SCHEDULED);
	}

	@Test
	void create_whenTelegramUserIdIsNotPositive_shouldThrowInvalidReminderException() {
		assertThatThrownBy(() -> reminderService.create(0L, "Покормить кота", NOW.plusSeconds(60)))
				.isInstanceOf(InvalidReminderException.class)
				.hasMessage("Telegram user id must be positive");
	}

	@Test
	void create_whenTextIsBlank_shouldThrowInvalidReminderException() {
		assertThatThrownBy(() -> reminderService.create(TELEGRAM_USER_ID, " ", NOW.plusSeconds(60)))
				.isInstanceOf(InvalidReminderException.class)
				.hasMessage("Reminder text must not be blank");
	}

	@Test
	void create_whenTextIsNull_shouldThrowInvalidReminderException() {
		assertThatThrownBy(() -> reminderService.create(TELEGRAM_USER_ID, null, NOW.plusSeconds(60)))
				.isInstanceOf(InvalidReminderException.class)
				.hasMessage("Reminder text must not be blank");
	}

	@Test
	void create_whenTextExceedsLimit_shouldThrowInvalidReminderException() {
		assertThatThrownBy(() -> reminderService.create(
				TELEGRAM_USER_ID,
				"a".repeat(501),
				NOW.plusSeconds(60)))
				.isInstanceOf(InvalidReminderException.class)
				.hasMessage("Reminder text must not exceed 500 characters");
	}

	@Test
	void create_whenTimeIsNotInFuture_shouldThrowInvalidReminderException() {
		assertThatThrownBy(() -> reminderService.create(TELEGRAM_USER_ID, "Покормить кота", NOW))
				.isInstanceOf(InvalidReminderException.class)
				.hasMessage("Reminder time must be in the future");
	}

	@Test
	void complete_whenReminderIsScheduled_shouldCompleteReminder() {
		var reminder = scheduledReminder();
		when(reminderRepository.findByIdAndTelegramUserId(REMINDER_ID, TELEGRAM_USER_ID))
				.thenReturn(Optional.of(reminder));

		var result = reminderService.complete(REMINDER_ID, TELEGRAM_USER_ID);

		assertThat(result.getStatus()).isEqualTo(ReminderStatus.COMPLETED);
		assertThat(result.getCompletedAt()).isEqualTo(NOW);
	}

	@Test
	void complete_whenReminderIsAlreadyCompleted_shouldKeepOriginalCompletionTime() {
		var reminder = scheduledReminder();
		var originalCompletionTime = NOW.minusSeconds(60);
		reminder.complete(originalCompletionTime);
		when(reminderRepository.findByIdAndTelegramUserId(REMINDER_ID, TELEGRAM_USER_ID))
				.thenReturn(Optional.of(reminder));

		var result = reminderService.complete(REMINDER_ID, TELEGRAM_USER_ID);

		assertThat(result.getCompletedAt()).isEqualTo(originalCompletionTime);
	}

	@Test
	void complete_whenReminderDoesNotBelongToUser_shouldThrowReminderNotFoundException() {
		when(reminderRepository.findByIdAndTelegramUserId(REMINDER_ID, TELEGRAM_USER_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> reminderService.complete(REMINDER_ID, TELEGRAM_USER_ID))
				.isInstanceOf(ReminderNotFoundException.class)
				.hasMessage("Reminder not found: " + REMINDER_ID);
	}

	@Test
	void reschedule_whenReminderIsScheduled_shouldChangeReminderTime() {
		var reminder = scheduledReminder();
		var newTime = NOW.plusSeconds(7200);
		when(reminderRepository.findByIdAndTelegramUserId(REMINDER_ID, TELEGRAM_USER_ID))
				.thenReturn(Optional.of(reminder));

		var result = reminderService.reschedule(REMINDER_ID, TELEGRAM_USER_ID, newTime);

		assertThat(result.getRemindAt()).isEqualTo(newTime);
	}

	@Test
	void reschedule_whenReminderIsCompleted_shouldThrowReminderAlreadyCompletedException() {
		var reminder = scheduledReminder();
		reminder.complete(NOW.minusSeconds(60));
		when(reminderRepository.findByIdAndTelegramUserId(REMINDER_ID, TELEGRAM_USER_ID))
				.thenReturn(Optional.of(reminder));

		assertThatThrownBy(() -> reminderService.reschedule(
				REMINDER_ID,
				TELEGRAM_USER_ID,
				NOW.plusSeconds(7200)))
				.isInstanceOf(ReminderAlreadyCompletedException.class)
				.hasMessage("Completed reminder cannot be rescheduled");
	}

	@Test
	void reschedule_whenTimeIsNotInFuture_shouldThrowInvalidReminderException() {
		var reminder = scheduledReminder();
		when(reminderRepository.findByIdAndTelegramUserId(REMINDER_ID, TELEGRAM_USER_ID))
				.thenReturn(Optional.of(reminder));

		assertThatThrownBy(() -> reminderService.reschedule(REMINDER_ID, TELEGRAM_USER_ID, NOW))
				.isInstanceOf(InvalidReminderException.class)
				.hasMessage("Reminder time must be in the future");
	}

	private Reminder scheduledReminder() {
		return Reminder.schedule(
				TELEGRAM_USER_ID,
				"Покормить кота",
				NOW.plusSeconds(3600),
				NOW.minusSeconds(3600));
	}
}
