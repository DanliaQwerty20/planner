package by.korchagin.planner.reminder.delivery.adapter;

import by.korchagin.planner.reminder.delivery.gateway.ReminderMessageSender;

public class UnconfiguredReminderMessageSender implements ReminderMessageSender {

	@Override
	public void send(long telegramUserId, String text) {
		throw new IllegalStateException("Telegram message sender is not configured");
	}
}
