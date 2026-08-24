package by.korchagin.planner.telegram.client;

import java.time.Duration;
import java.util.List;

import by.korchagin.planner.telegram.dto.TelegramUpdate;

public interface TelegramUpdateClient {

	void deleteWebhook(boolean dropPendingUpdates);

	List<TelegramUpdate> getUpdates(Long offset, Duration timeout);
}
