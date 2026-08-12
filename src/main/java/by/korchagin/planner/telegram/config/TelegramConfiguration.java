package by.korchagin.planner.telegram.config;

import by.korchagin.planner.telegram.client.TelegramClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TelegramConfiguration {

	@Bean
	@ConditionalOnMissingBean(TelegramClient.class)
	TelegramClient telegramClient() {
		return new TelegramClient() {
			@Override
			public void sendMessage(long chatId, String text) {
				throw new IllegalStateException("Telegram client is not configured");
			}

			@Override
			public void sendConfirmation(long chatId, String text, String confirmationData) {
				throw new IllegalStateException("Telegram client is not configured");
			}

			@Override
			public byte[] downloadFile(String fileId) {
				throw new IllegalStateException("Telegram client is not configured");
			}
		};
	}
}
