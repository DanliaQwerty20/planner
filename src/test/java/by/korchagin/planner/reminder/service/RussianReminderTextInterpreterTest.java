package by.korchagin.planner.reminder.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import by.korchagin.planner.reminder.dto.ReminderClarification;
import by.korchagin.planner.reminder.dto.ReminderInterpretation;
import org.junit.jupiter.api.Test;

class RussianReminderTextInterpreterTest {

	private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Moscow");
	private static final Clock CLOCK = Clock.fixed(
			Instant.parse("2026-08-13T09:00:00Z"),
			TIME_ZONE);

	private final RussianReminderTextInterpreter interpreter =
			new RussianReminderTextInterpreter(CLOCK, TIME_ZONE);

	@Test
	void interpret_whenTomorrowAndTimeArePresent_shouldReturnReminderInterpretation() {
		var result = interpreter.interpret("Завтра в 15:00 покормить кота");

		assertThat(result).isEqualTo(new ReminderInterpretation(
				"Покормить кота",
				Instant.parse("2026-08-14T12:00:00Z"),
				TIME_ZONE));
	}

	@Test
	void interpret_whenTodayAndTimeArePresent_shouldReturnReminderInterpretation() {
		var result = interpreter.interpret("сегодня в 18:30 написать Диме про долг");

		assertThat(result).isEqualTo(new ReminderInterpretation(
				"Написать Диме про долг",
				Instant.parse("2026-08-13T15:30:00Z"),
				TIME_ZONE));
	}

	@Test
	void interpret_whenDayIsMissing_shouldAskForDay() {
		var result = interpreter.interpret("В 15:00 покормить кота");

		assertThat(result).isEqualTo(new ReminderClarification(
				"На какой день поставить напоминание?"));
	}

	@Test
	void interpret_whenTimeIsMissing_shouldAskForTime() {
		var result = interpreter.interpret("Завтра покормить кота");

		assertThat(result).isEqualTo(new ReminderClarification("Во сколько напомнить?"));
	}

	@Test
	void interpret_whenTodayTimeHasPassed_shouldAskForFutureTime() {
		var result = interpreter.interpret("Сегодня в 10:00 покормить кота");

		assertThat(result).isEqualTo(new ReminderClarification(
				"Это время уже прошло. На какое время поставить напоминание?"));
	}

	@Test
	void interpret_whenTimeIsWrittenNaturally_shouldReturnReminderInterpretation() {
		var result = interpreter.interpret("завтра в 9 утра позвонить маме");

		assertThat(result).isEqualTo(new ReminderInterpretation(
				"Позвонить маме",
				Instant.parse("2026-08-14T06:00:00Z"),
				TIME_ZONE));
	}

	@Test
	void interpret_whenWeekdayIsPresent_shouldUseNextMatchingDay() {
		var result = interpreter.interpret("в пятницу в 18:30 купить корм коту");

		assertThat(result).isEqualTo(new ReminderInterpretation(
				"Купить корм коту",
				Instant.parse("2026-08-14T15:30:00Z"),
				TIME_ZONE));
	}

	@Test
	void interpret_whenRelativeDelayIsPresent_shouldUseCurrentTimeAsBase() {
		var result = interpreter.interpret("напомни через 10 минут выключить духовку");

		assertThat(result).isEqualTo(new ReminderInterpretation(
				"Выключить духовку",
				Instant.parse("2026-08-13T09:10:00Z"),
				TIME_ZONE));
	}

	@Test
	void interpret_whenClarificationMessagesAreCombined_shouldUseAllProvidedDetails() {
		var result = interpreter.interpret("завтра встретиться с дядей\nпримерно в 14 00");

		assertThat(result).isEqualTo(new ReminderInterpretation(
				"Встретиться с дядей",
				Instant.parse("2026-08-14T11:00:00Z"),
				TIME_ZONE));
	}

	@Test
	void interpret_whenOneMessageContainsSeveralTimes_shouldAskForOneReminderAtATime() {
		var result = interpreter.interpret(
				"завтра в 12:00 купить хлеб и в 18:00 позвонить врачу");

		assertThat(result).isEqualTo(new ReminderClarification(
				"Я вижу несколько времён. Отправь каждое напоминание отдельным сообщением.",
				false));
	}
}
