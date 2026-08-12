package by.korchagin.planner.reminder.delivery.gateway;

public interface ReminderMessageSender {

	void send(long telegramUserId, String text);
}
