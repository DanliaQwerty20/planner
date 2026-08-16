package by.korchagin.planner.voice.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("planner.voice.transcription")
public record SpeechTranscriptionProperties(
		boolean enabled,
		String apiKey,
		@DefaultValue("https://api.openai.com") URI apiBaseUrl,
		@DefaultValue("gpt-4o-mini-transcribe") String model,
		@DefaultValue("ru") String language,
		@DefaultValue("5s") Duration connectTimeout,
		@DefaultValue("60s") Duration readTimeout) {

	public void validateEnabledConfiguration() {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException(
					"OpenAI API key must be configured when speech transcription is enabled");
		}
		if (model == null || model.isBlank()) {
			throw new IllegalStateException("OpenAI transcription model must not be blank");
		}
		if (language == null || language.isBlank()) {
			throw new IllegalStateException("OpenAI transcription language must not be blank");
		}
		if (connectTimeout.isZero() || connectTimeout.isNegative()) {
			throw new IllegalStateException("OpenAI connect timeout must be positive");
		}
		if (readTimeout.isZero() || readTimeout.isNegative()) {
			throw new IllegalStateException("OpenAI read timeout must be positive");
		}
	}

	@Override
	public String toString() {
		return "SpeechTranscriptionProperties[enabled=%s, apiKey=<redacted>, apiBaseUrl=%s, model=%s, language=%s]"
				.formatted(enabled, apiBaseUrl, model, language);
	}
}
