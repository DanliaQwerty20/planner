package by.korchagin.planner.telegram.client;

import by.korchagin.planner.telegram.dto.TelegramReminderActions;

public interface TelegramClient {

	void sendMessage(long chatId, String text);

	void sendReminder(long chatId, String text, TelegramReminderActions actions);

	void sendConfirmation(long chatId, String text, String confirmationData);

	byte[] downloadFile(String fileId);
}
