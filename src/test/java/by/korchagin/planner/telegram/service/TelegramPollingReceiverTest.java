package by.korchagin.planner.telegram.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import by.korchagin.planner.telegram.client.TelegramUpdateClient;
import by.korchagin.planner.telegram.config.TelegramProperties;
import by.korchagin.planner.telegram.config.TelegramUpdateMode;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramPollingReceiverTest {

	private static final Duration POLLING_TIMEOUT = Duration.ofSeconds(10);

	@Mock
	private TelegramUpdateClient telegramUpdateClient;

	@Mock
	private TelegramUpdateService telegramUpdateService;

	private TelegramPollingReceiver telegramPollingReceiver;

	@BeforeEach
	void setUp() {
		var properties = new TelegramProperties(
				true,
				"test-token",
				null,
				URI.create("https://api.telegram.org"),
				TelegramUpdateMode.POLLING,
				POLLING_TIMEOUT,
				Duration.ofMillis(500));
		telegramPollingReceiver = new TelegramPollingReceiver(
				telegramUpdateClient,
				telegramUpdateService,
				properties);
	}

	@Test
	void receiveUpdates_onFirstPoll_shouldDeleteWebhookAndHandleUpdatesInOrder() {
		var firstUpdate = new TelegramUpdate(40L, null, null);
		var secondUpdate = new TelegramUpdate(41L, null, null);
		when(telegramUpdateClient.getUpdates(null, POLLING_TIMEOUT))
				.thenReturn(List.of(firstUpdate, secondUpdate));

		telegramPollingReceiver.receiveUpdates();

		var processingOrder = inOrder(telegramUpdateClient, telegramUpdateService);
		processingOrder.verify(telegramUpdateClient).deleteWebhook(false);
		processingOrder.verify(telegramUpdateClient).getUpdates(null, POLLING_TIMEOUT);
		processingOrder.verify(telegramUpdateService).handle(firstUpdate);
		processingOrder.verify(telegramUpdateService).handle(secondUpdate);
	}

	@Test
	void receiveUpdates_onNextPoll_shouldContinueAfterLastHandledUpdate() {
		var update = new TelegramUpdate(40L, null, null);
		when(telegramUpdateClient.getUpdates(null, POLLING_TIMEOUT)).thenReturn(List.of(update));
		when(telegramUpdateClient.getUpdates(41L, POLLING_TIMEOUT)).thenReturn(List.of());

		telegramPollingReceiver.receiveUpdates();
		telegramPollingReceiver.receiveUpdates();

		verify(telegramUpdateClient, times(1)).deleteWebhook(false);
		verify(telegramUpdateClient).getUpdates(41L, POLLING_TIMEOUT);
	}
}
