package by.korchagin.planner.telegram.config;

import java.net.URI;
import java.time.Duration;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("planner.telegram")
public record TelegramProperties(
		boolean enabled,
		String botToken,
		String webhookSecret,
		@DefaultValue("https://api.telegram.org") URI apiBaseUrl,
		@DefaultValue("WEBHOOK") TelegramUpdateMode updateMode,
		@DefaultValue("10s") Duration pollingTimeout,
		@DefaultValue("500ms") Duration pollingDelay) {

	private static final Pattern WEBHOOK_SECRET_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,256}");

	public void validateEnabledConfiguration() {
		if (botToken == null || botToken.isBlank()) {
			throw new IllegalStateException("Telegram bot token must be configured when Telegram is enabled");
		}
		if (updateMode == TelegramUpdateMode.WEBHOOK
				&& (webhookSecret == null || !WEBHOOK_SECRET_PATTERN.matcher(webhookSecret).matches())) {
			throw new IllegalStateException(
					"Telegram webhook secret must contain 1-256 letters, digits, underscores, or hyphens");
		}
		if (pollingTimeout == null || pollingTimeout.compareTo(Duration.ofSeconds(1)) < 0) {
			throw new IllegalStateException("Telegram polling timeout must be at least one second");
		}
		if (pollingDelay == null || pollingDelay.isNegative()) {
			throw new IllegalStateException("Telegram polling delay must not be negative");
		}
	}
}
