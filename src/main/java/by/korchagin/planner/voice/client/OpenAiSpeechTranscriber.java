package by.korchagin.planner.voice.client;

import java.net.URI;

import by.korchagin.planner.voice.client.dto.OpenAiTranscriptionResponse;
import by.korchagin.planner.voice.config.SpeechTranscriptionProperties;
import by.korchagin.planner.voice.exception.SpeechTranscriptionException;
import by.korchagin.planner.voice.service.SpeechTranscriber;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class OpenAiSpeechTranscriber implements SpeechTranscriber {

	private static final MediaType OGG_AUDIO = MediaType.parseMediaType("audio/ogg");
	private static final String AUDIO_FILENAME = "voice.ogg";
	private static final String TRANSCRIPTIONS_PATH = "/v1/audio/transcriptions";

	private final RestClient restClient;
	private final SpeechTranscriptionProperties properties;
	private final String transcriptionsUrl;

	public OpenAiSpeechTranscriber(
			RestClient.Builder restClientBuilder,
			SpeechTranscriptionProperties properties) {
		var requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());
		this.restClient = restClientBuilder.requestFactory(requestFactory).build();
		this.properties = properties;
		this.transcriptionsUrl = withoutTrailingSlash(properties.apiBaseUrl()) + TRANSCRIPTIONS_PATH;
	}

	@Override
	public String transcribe(byte[] audio) {
		if (audio == null || audio.length == 0) {
			throw new SpeechTranscriptionException("Audio must not be empty");
		}

		var response = requestTranscription(audio);
		if (response == null || response.text() == null || response.text().isBlank()) {
			throw new SpeechTranscriptionException("OpenAI transcription response is empty");
		}
		return response.text().strip();
	}

	private OpenAiTranscriptionResponse requestTranscription(byte[] audio) {
		var multipart = new LinkedMultiValueMap<String, Object>();
		var audioHeaders = new HttpHeaders();
		audioHeaders.setContentType(OGG_AUDIO);
		multipart.add("file", new HttpEntity<>(audioResource(audio), audioHeaders));
		multipart.add("model", properties.model());
		multipart.add("language", properties.language());

		try {
			return restClient.post()
					.uri(URI.create(transcriptionsUrl))
					.headers(headers -> headers.setBearerAuth(properties.apiKey()))
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(multipart)
					.retrieve()
					.body(OpenAiTranscriptionResponse.class);
		}
		catch (RestClientException | IllegalArgumentException exception) {
			throw new SpeechTranscriptionException("OpenAI transcription request failed");
		}
	}

	private ByteArrayResource audioResource(byte[] audio) {
		return new ByteArrayResource(audio) {
			@Override
			public String getFilename() {
				return AUDIO_FILENAME;
			}
		};
	}

	private static String withoutTrailingSlash(URI uri) {
		var value = uri.toString();
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}
}
