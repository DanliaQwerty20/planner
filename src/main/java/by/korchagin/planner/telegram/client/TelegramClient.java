package by.korchagin.planner.telegram.client;

public interface TelegramClient {

	void sendMessage(long telegramUserId, String text);
}
