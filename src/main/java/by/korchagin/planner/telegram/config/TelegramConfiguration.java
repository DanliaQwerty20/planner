package by.korchagin.planner.telegram.config;

import by.korchagin.planner.telegram.client.TelegramBotApiClient;
import by.korchagin.planner.telegram.client.TelegramClient;
import by.korchagin.planner.telegram.dto.TelegramReminderActions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "planner.telegram", name = "enabled", havingValue = "true")
	TelegramClient telegramClient(TelegramProperties telegramProperties) {
		telegramProperties.validateEnabledConfiguration();
		return new TelegramBotApiClient(RestClient.builder(), telegramProperties);
	}

	@Bean
	@ConditionalOnProperty(prefix = "planner.telegram", name = "enabled", havingValue = "false", matchIfMissing = true)
	TelegramClient unconfiguredTelegramClient() {
		return new TelegramClient() {
			@Override
			public void sendMessage(long chatId, String text) {
				throw new IllegalStateException("Telegram client is not configured");
			}

			@Override
			public void sendReminder(long chatId, String text, TelegramReminderActions actions) {
				throw new IllegalStateException("Telegram client is not configured");
			}

			@Override
			public void sendConfirmation(long chatId, String text, String confirmationData) {
				throw new IllegalStateException("Telegram client is not configured");
			}

			@Override
			public void answerCallbackQuery(String callbackQueryId) {
				throw new IllegalStateException("Telegram client is not configured");
			}

			@Override
			public byte[] downloadFile(String fileId) {
				throw new IllegalStateException("Telegram client is not configured");
			}
		};
	}
}
