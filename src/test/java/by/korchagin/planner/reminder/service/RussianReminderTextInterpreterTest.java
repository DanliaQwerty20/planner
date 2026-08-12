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
}
