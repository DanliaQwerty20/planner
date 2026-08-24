package by.korchagin.planner.telegram.service;

import by.korchagin.planner.telegram.client.TelegramUpdateClient;
import by.korchagin.planner.telegram.config.TelegramProperties;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import by.korchagin.planner.telegram.exception.TelegramApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "planner.telegram", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "planner.telegram", name = "update-mode", havingValue = "polling")
public class TelegramPollingReceiver {

	private final TelegramUpdateClient telegramUpdateClient;
	private final TelegramUpdateService telegramUpdateService;
	private final TelegramProperties telegramProperties;

	private Long nextOffset;
	private boolean webhookDeleted;

	@Scheduled(
			fixedDelayString = "${planner.telegram.polling-delay:500ms}",
			scheduler = "telegramPollingTaskScheduler")
	public void receiveUpdates() {
		try {
			ensureWebhookDeleted();
			telegramUpdateClient.getUpdates(nextOffset, telegramProperties.pollingTimeout())
					.forEach(this::handleUpdate);
		}
		catch (TelegramApiException exception) {
			log.warn("Telegram long polling request failed; it will be retried", exception);
		}
	}

	private void ensureWebhookDeleted() {
		if (webhookDeleted) {
			return;
		}

		telegramUpdateClient.deleteWebhook(false);
		webhookDeleted = true;
		log.info("Telegram webhook removed; long polling is active");
	}

	private void handleUpdate(TelegramUpdate update) {
		try {
			telegramUpdateService.handle(update);
		}
		catch (RuntimeException exception) {
			log.error("Telegram update {} could not be handled", update.updateId(), exception);
		}
		finally {
			nextOffset = update.updateId() + 1;
		}
	}
}
