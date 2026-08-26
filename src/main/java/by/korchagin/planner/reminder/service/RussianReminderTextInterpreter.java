package by.korchagin.planner.reminder.service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import by.korchagin.planner.reminder.dto.ReminderClarification;
import by.korchagin.planner.reminder.dto.ReminderInterpretation;
import by.korchagin.planner.reminder.dto.ReminderInterpretationResult;

public class RussianReminderTextInterpreter implements ReminderTextInterpreter {

	private static final Pattern RELATIVE_DELAY = Pattern.compile(
			"(?iuU)\\bчерез\\s+(\\d{1,4})\\s*(мин(?:ут(?:у|ы)?)?|час(?:а|ов)?)\\b");
	private static final Pattern RELATIVE_DAY = Pattern.compile(
			"(?iuU)\\b(сегодня|завтра|послезавтра)\\b");
	private static final Pattern WEEKDAY = Pattern.compile(
			"(?iuU)(?:\\bв\\s+)?\\b(понедельник|вторник|сред[ау]|четверг|пятниц[ау]|суббот[ау]|воскресенье)\\b");
	private static final Pattern EXPLICIT_DATE = Pattern.compile(
			"(?<!\\d)([0-3]?\\d)[./]([01]?\\d)(?:[./](\\d{4}))?(?!\\d)");
	private static final Pattern TIME = Pattern.compile(
			"(?iuU)(?<!\\d)(?:в\\s+)?([01]?\\d|2[0-3])(?:\\s*[:.]\\s*|\\s+)([0-5]\\d)"
					+ "(?:\\s*(утра|дня|вечера|ночи))?(?!\\d)"
					+ "|(?<!\\d)(?:в\\s+)?([01]?\\d|2[0-3])\\s*(утра|дня|вечера|ночи)\\b");
	private static final Pattern FILLER_WORDS = Pattern.compile(
			"(?iuU)\\b(?:напомни|напомнить|поставь|поставить|напоминание|мне|надо|нужно|примерно|пожалуйста)\\b");
	private static final Pattern EXTRA_WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern EDGE_PUNCTUATION = Pattern.compile("^[\\s,;:.!?—-]+|[\\s,;:.!?—-]+$");

	private static final String MULTIPLE_VALUES_QUESTION =
			"Я вижу несколько времён. Отправь каждое напоминание отдельным сообщением.";

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

		var normalizedText = normalize(text);
		var latestMessage = latestMessage(normalizedText);
		if (findTimes(latestMessage).size() > 1 || findDates(latestMessage).size() > 1) {
			return new ReminderClarification(MULTIPLE_VALUES_QUESTION, false);
		}

		var delay = lastMatch(RELATIVE_DELAY, normalizedText);
		if (delay != null) {
			return interpretDelay(normalizedText, delay);
		}

		var dates = findDates(normalizedText);
		var times = findTimes(normalizedText);
		var action = extractAction(normalizedText);
		if (dates.isEmpty()) {
			return new ReminderClarification("На какой день поставить напоминание?");
		}
		if (times.isEmpty()) {
			return new ReminderClarification("Во сколько напомнить?");
		}
		if (action.isBlank()) {
			return new ReminderClarification("Что нужно напомнить?");
		}

		var date = dates.getLast().date();
		var time = times.getLast().time();
		var remindAt = date.atTime(time).atZone(timeZone).toInstant();
		if (!remindAt.isAfter(clock.instant()) && dates.getLast().nextOccurrenceWhenPast()) {
			remindAt = date.plusWeeks(1).atTime(time).atZone(timeZone).toInstant();
		}
		if (!remindAt.isAfter(clock.instant())) {
			return new ReminderClarification(
					"Это время уже прошло. На какое время поставить напоминание?");
		}
		return new ReminderInterpretation(capitalize(action), remindAt, timeZone);
	}

	private ReminderInterpretationResult interpretDelay(String text, MatchResult delay) {
		var amount = Long.parseLong(delay.group(1));
		var unit = delay.group(2).toLowerCase(Locale.ROOT);
		var duration = unit.startsWith("час") ? Duration.ofHours(amount) : Duration.ofMinutes(amount);
		if (duration.isZero()) {
			return new ReminderClarification("Через сколько времени напомнить?");
		}

		var action = extractAction(text);
		if (action.isBlank()) {
			return new ReminderClarification("Что нужно напомнить?");
		}
		return new ReminderInterpretation(capitalize(action), clock.instant().plus(duration), timeZone);
	}

	private List<DateMatch> findDates(String text) {
		var matches = new ArrayList<DateMatch>();
		var today = LocalDate.now(clock.withZone(timeZone));

		RELATIVE_DAY.matcher(text).results().forEach(match -> {
			var days = switch (match.group(1).toLowerCase(Locale.ROOT)) {
				case "завтра" -> 1;
				case "послезавтра" -> 2;
				default -> 0;
			};
			matches.add(new DateMatch(match.start(), today.plusDays(days), false));
		});
		WEEKDAY.matcher(text).results().forEach(match -> {
			var targetDay = weekday(match.group(1));
			var daysAhead = Math.floorMod(targetDay.getValue() - today.getDayOfWeek().getValue(), 7);
			matches.add(new DateMatch(match.start(), today.plusDays(daysAhead), daysAhead == 0));
		});
		EXPLICIT_DATE.matcher(text).results().forEach(match -> {
			try {
				var year = match.group(3) == null ? today.getYear() : Integer.parseInt(match.group(3));
				var date = LocalDate.of(
						year,
						Integer.parseInt(match.group(2)),
						Integer.parseInt(match.group(1)));
				if (match.group(3) == null && date.isBefore(today)) {
					date = date.plusYears(1);
				}
				matches.add(new DateMatch(match.start(), date, false));
			}
			catch (DateTimeException ignored) {
				// The invalid calendar value stays unparsed and triggers a date clarification.
			}
		});
		matches.sort((left, right) -> Integer.compare(left.position(), right.position()));
		return matches;
	}

	private List<TimeMatch> findTimes(String text) {
		var matches = new ArrayList<TimeMatch>();
		TIME.matcher(text).results().forEach(match -> matches.add(
				new TimeMatch(match.start(), toTime(match))));
		return matches;
	}

	private LocalTime toTime(MatchResult match) {
		var firstForm = match.group(1) != null;
		var hour = Integer.parseInt(firstForm ? match.group(1) : match.group(4));
		var minute = firstForm ? Integer.parseInt(match.group(2)) : 0;
		var period = firstForm ? match.group(3) : match.group(5);
		if (period != null) {
			hour = applyPeriod(hour, period.toLowerCase(Locale.ROOT));
		}
		return LocalTime.of(hour, minute);
	}

	private int applyPeriod(int hour, String period) {
		return switch (period) {
			case "дня", "вечера" -> hour == 12 ? 12 : hour + 12;
			case "ночи" -> hour == 12 ? 0 : hour;
			default -> hour;
		};
	}

	private DayOfWeek weekday(String value) {
		var normalized = value.toLowerCase(Locale.ROOT);
		if (normalized.startsWith("понедельник")) {
			return DayOfWeek.MONDAY;
		}
		if (normalized.startsWith("вторник")) {
			return DayOfWeek.TUESDAY;
		}
		if (normalized.startsWith("сред")) {
			return DayOfWeek.WEDNESDAY;
		}
		if (normalized.startsWith("четверг")) {
			return DayOfWeek.THURSDAY;
		}
		if (normalized.startsWith("пятниц")) {
			return DayOfWeek.FRIDAY;
		}
		if (normalized.startsWith("суббот")) {
			return DayOfWeek.SATURDAY;
		}
		return DayOfWeek.SUNDAY;
	}

	private String extractAction(String text) {
		var withoutDateAndTime = RELATIVE_DELAY.matcher(text).replaceAll(" ");
		withoutDateAndTime = RELATIVE_DAY.matcher(withoutDateAndTime).replaceAll(" ");
		withoutDateAndTime = WEEKDAY.matcher(withoutDateAndTime).replaceAll(" ");
		withoutDateAndTime = EXPLICIT_DATE.matcher(withoutDateAndTime).replaceAll(" ");
		withoutDateAndTime = TIME.matcher(withoutDateAndTime).replaceAll(" ");
		withoutDateAndTime = FILLER_WORDS.matcher(withoutDateAndTime).replaceAll(" ");
		var compact = EXTRA_WHITESPACE.matcher(withoutDateAndTime).replaceAll(" ").strip();
		return EDGE_PUNCTUATION.matcher(compact).replaceAll("").strip();
	}

	private MatchResult lastMatch(Pattern pattern, String text) {
		return pattern.matcher(text).results().reduce((first, second) -> second).orElse(null);
	}

	private String normalize(String text) {
		return text.replace('\u00a0', ' ').strip();
	}

	private String latestMessage(String text) {
		var separator = text.lastIndexOf('\n');
		return separator < 0 ? text : text.substring(separator + 1);
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

	private record DateMatch(int position, LocalDate date, boolean nextOccurrenceWhenPast) {
	}

	private record TimeMatch(int position, LocalTime time) {
	}
}
