package by.korchagin.planner.voice.config;

import by.korchagin.planner.voice.client.OpenAiSpeechTranscriber;
import by.korchagin.planner.voice.service.SpeechTranscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SpeechTranscriptionProperties.class)
public class SpeechTranscriptionConfiguration {

	@Bean
	@ConditionalOnProperty(
			prefix = "planner.voice.transcription",
			name = "enabled",
			havingValue = "true")
	SpeechTranscriber speechTranscriber(SpeechTranscriptionProperties properties) {
		properties.validateEnabledConfiguration();
		return new OpenAiSpeechTranscriber(RestClient.builder(), properties);
	}

	@Bean
	@ConditionalOnProperty(
			prefix = "planner.voice.transcription",
			name = "enabled",
			havingValue = "false",
			matchIfMissing = true)
	SpeechTranscriber unconfiguredSpeechTranscriber() {
		return new SpeechTranscriber() {
			@Override
			public boolean isAvailable() {
				return false;
			}

			@Override
			public String transcribe(byte[] audio) {
				throw new IllegalStateException("Speech transcriber is not configured");
			}
		};
	}
}
