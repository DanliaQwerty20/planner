package by.korchagin.planner.reminder.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import by.korchagin.planner.reminder.dto.ReminderClarification;
import by.korchagin.planner.reminder.dto.ReminderInterpretation;
import by.korchagin.planner.reminder.dto.ReminderInterpretationResult;

public class RussianReminderTextInterpreter implements ReminderTextInterpreter {

	private static final Pattern COMPLETE_REMINDER = Pattern.compile(
			"^\\s*(сегодня|завтра)\\s+в\\s+([01]?\\d|2[0-3]):([0-5]\\d)\\s+(.+?)\\s*$",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern DAY = Pattern.compile(
			"(?:^|\\s)(сегодня|завтра)(?:\\s|$)",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	private static final Pattern TIME = Pattern.compile(
			"(?:^|\\s)в\\s+([01]?\\d|2[0-3]):([0-5]\\d)(?:\\s|$)",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private final Clock clock;
	private final ZoneId timeZone;

	public RussianReminderTextInterpreter(Clock clock, ZoneId timeZone) {
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.timeZone = Objects.requireNonNull(timeZone, "timeZone must not be null");
	}

	@Override
	public ReminderInterpretationResult interpret(String text) {
		if (text == null || text.isBlank()) {
			return new ReminderClarification("Что нужно напомнить?");
		}

		var matcher = COMPLETE_REMINDER.matcher(text);
		if (matcher.matches()) {
			return toInterpretation(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
		}
		if (!DAY.matcher(text).find()) {
			return new ReminderClarification("На какой день поставить напоминание?");
		}
		if (!TIME.matcher(text).find()) {
			return new ReminderClarification("Во сколько напомнить?");
		}
		return new ReminderClarification("Скажи одним сообщением, когда и что нужно напомнить.");
	}

	private ReminderInterpretationResult toInterpretation(
			String relativeDay,
			String hour,
			String minute,
			String action) {
		var date = LocalDate.now(clock);
		if (relativeDay.toLowerCase(Locale.ROOT).equals("завтра")) {
			date = date.plusDays(1);
		}
		var time = LocalTime.of(Integer.parseInt(hour), Integer.parseInt(minute));
		var remindAt = date.atTime(time).atZone(timeZone).toInstant();
		if (!remindAt.isAfter(clock.instant())) {
			return new ReminderClarification(
					"Это время уже прошло. На какое время поставить напоминание?");
		}
		return new ReminderInterpretation(capitalize(action.strip()), remindAt, timeZone);
	}

	private String capitalize(String text) {
		if (text.isEmpty()) {
			return text;
		}
		var firstCodePoint = text.codePointAt(0);
		var firstCharacterLength = Character.charCount(firstCodePoint);
		var capitalizedFirstCharacter = new String(Character.toChars(Character.toUpperCase(firstCodePoint)));
		return capitalizedFirstCharacter + text.substring(firstCharacterLength);
	}
}
