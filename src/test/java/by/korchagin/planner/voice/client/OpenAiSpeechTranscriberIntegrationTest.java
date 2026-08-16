package by.korchagin.planner.voice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.voice.exception.SpeechTranscriptionException;
import by.korchagin.planner.voice.service.SpeechTranscriber;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
		"planner.voice.transcription.enabled=true",
		"planner.voice.transcription.api-key=test-api-key",
		"planner.voice.transcription.model=gpt-4o-mini-transcribe",
		"planner.voice.transcription.language=ru"
})
@Import(TestcontainersConfiguration.class)
class OpenAiSpeechTranscriberIntegrationTest {

	private static final byte[] VOICE_AUDIO = "ogg-opus-audio".getBytes(StandardCharsets.UTF_8);
	private static final BlockingQueue<RecordedRequest> REQUESTS = new LinkedBlockingQueue<>();
	private static final HttpServer OPENAI_API = startOpenAiApi();

	@Autowired
	private SpeechTranscriber speechTranscriber;

	@DynamicPropertySource
	static void openAiApiProperties(DynamicPropertyRegistry registry) {
		registry.add(
				"planner.voice.transcription.api-base-url",
				() -> "http://localhost:" + OPENAI_API.getAddress().getPort());
	}

	@BeforeEach
	void setUp() {
		REQUESTS.clear();
	}

	@AfterAll
	static void stopOpenAiApi() {
		OPENAI_API.stop(0);
	}

	@Test
	void transcribe_shouldSendOggMultipartAndReturnTrimmedRussianText() throws InterruptedException {
		var transcription = speechTranscriber.transcribe(VOICE_AUDIO);

		assertThat(transcription).isEqualTo("Завтра в 15:00 покормить кота");
		var request = nextRequest();
		assertThat(request.path()).isEqualTo("/v1/audio/transcriptions");
		assertThat(request.authorization()).isEqualTo("Bearer test-api-key");
		assertThat(request.contentType()).startsWith("multipart/form-data;boundary=");
		assertThat(request.body())
				.contains("name=\"file\"; filename=\"voice.ogg\"")
				.contains("Content-Type: audio/ogg")
				.contains("ogg-opus-audio")
				.contains("name=\"model\"")
				.contains("gpt-4o-mini-transcribe")
				.contains("name=\"language\"")
				.contains("ru");
	}

	@Test
	void transcribe_whenAudioIsEmpty_shouldRejectWithoutCallingApi() {
		assertThatThrownBy(() -> speechTranscriber.transcribe(new byte[0]))
				.isInstanceOf(SpeechTranscriptionException.class)
				.hasMessage("Audio must not be empty");

		assertThat(REQUESTS).isEmpty();
	}

	@Test
	void transcribe_whenApiRejectsRequest_shouldNotExposeApiKey() {
		var rejectedAudio = "force-error".getBytes(StandardCharsets.UTF_8);

		assertThatThrownBy(() -> speechTranscriber.transcribe(rejectedAudio))
				.isInstanceOf(SpeechTranscriptionException.class)
				.hasMessage("OpenAI transcription request failed")
				.hasMessageNotContaining("test-api-key");
	}

	private RecordedRequest nextRequest() throws InterruptedException {
		var request = REQUESTS.poll(5, TimeUnit.SECONDS);
		assertThat(request).as("OpenAI transcription request").isNotNull();
		return request;
	}

	private static HttpServer startOpenAiApi() {
		try {
			var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/", OpenAiSpeechTranscriberIntegrationTest::handleRequest);
			server.start();
			return server;
		}
		catch (IOException exception) {
			throw new IllegalStateException("Failed to start OpenAI API test server", exception);
		}
	}

	private static void handleRequest(HttpExchange exchange) throws IOException {
		var request = new RecordedRequest(
				exchange.getRequestURI().getPath(),
				exchange.getRequestHeaders().getFirst("Authorization"),
				exchange.getRequestHeaders().getFirst("Content-Type"),
				new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
		REQUESTS.add(request);

		var isRejected = request.body().contains("force-error");
		var response = isRejected
				? "{\"error\":{\"message\":\"invalid audio\"}}".getBytes(StandardCharsets.UTF_8)
				: "{\"text\":\"  Завтра в 15:00 покормить кота  \"}".getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().put("Content-Type", List.of("application/json"));
		exchange.sendResponseHeaders(isRejected ? 400 : 200, response.length);
		try (var responseBody = exchange.getResponseBody()) {
			responseBody.write(response);
		}
	}

	private record RecordedRequest(
			String path,
			String authorization,
			String contentType,
			String body) {
	}
}
