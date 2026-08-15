package by.korchagin.planner.telegram.client;

public interface TelegramClient {

	void sendMessage(long chatId, String text);

	void sendReminder(long chatId, String text, String completionData);

	void sendConfirmation(long chatId, String text, String confirmationData);

	byte[] downloadFile(String fileId);
}
