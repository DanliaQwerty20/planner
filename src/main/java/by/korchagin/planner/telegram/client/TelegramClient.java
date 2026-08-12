package by.korchagin.planner.telegram.client;

public interface TelegramClient {

	void sendMessage(long chatId, String text);

	void sendConfirmation(long chatId, String text, String confirmationData);
}
