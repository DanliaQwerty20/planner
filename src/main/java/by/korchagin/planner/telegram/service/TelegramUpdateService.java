package by.korchagin.planner.telegram.service;

import java.time.format.DateTimeFormatter;

import by.korchagin.planner.reminder.dto.ReminderInterpretation;
import by.korchagin.planner.reminder.service.ReminderTextInterpreter;
import by.korchagin.planner.telegram.client.TelegramClient;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramUpdateService {

	private static final DateTimeFormatter DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");

	private final ReminderTextInterpreter reminderTextInterpreter;
	private final TelegramClient telegramClient;

	public void handle(TelegramUpdate update) {
		var message = update.message();
		var interpretation = reminderTextInterpreter.interpret(message.text());
		telegramClient.sendMessage(message.chat().id(), formatPreview(interpretation));
	}

	private String formatPreview(ReminderInterpretation interpretation) {
		var localTime = interpretation.remindAt().atZone(interpretation.timeZone());
		return "Проверь напоминание:\n%s — %s"
				.formatted(DATE_TIME_FORMATTER.format(localTime), interpretation.text());
	}
}
