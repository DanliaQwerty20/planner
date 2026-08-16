package by.korchagin.planner.telegram.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import by.korchagin.planner.telegram.config.TelegramProperties;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramWebhookService {

	private final TelegramProperties telegramProperties;
	private final TelegramUpdateService telegramUpdateService;

	public boolean handle(String providedSecret, TelegramUpdate update) {
		if (!hasValidSecret(providedSecret)) {
			return false;
		}

		telegramUpdateService.handle(update);
		return true;
	}

	private boolean hasValidSecret(String providedSecret) {
		if (providedSecret == null || telegramProperties.webhookSecret() == null) {
			return false;
		}

		var expected = telegramProperties.webhookSecret().getBytes(StandardCharsets.UTF_8);
		var actual = providedSecret.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expected, actual);
	}
}
