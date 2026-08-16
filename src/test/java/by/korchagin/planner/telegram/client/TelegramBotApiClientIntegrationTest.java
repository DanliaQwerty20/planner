package by.korchagin.planner.telegram.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.telegram.dto.TelegramReminderActions;
import by.korchagin.planner.telegram.exception.TelegramApiException;
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
		"planner.telegram.enabled=true",
		"planner.telegram.bot-token=test-token",
		"planner.telegram.webhook-secret=test-webhook-secret"
})
@Import(TestcontainersConfiguration.class)
class TelegramBotApiClientIntegrationTest {

	private static final byte[] VOICE_AUDIO = "ogg-opus-audio".getBytes(StandardCharsets.UTF_8);
	private static final BlockingQueue<RecordedRequest> REQUESTS = new LinkedBlockingQueue<>();
	private static final HttpServer TELEGRAM_API = startTelegramApi();

	@Autowired
	private TelegramClient telegramClient;

	@DynamicPropertySource
	static void telegramApiProperties(DynamicPropertyRegistry registry) {
		registry.add(
				"planner.telegram.api-base-url",
				() -> "http://localhost:" + TELEGRAM_API.getAddress().getPort());
	}

	@BeforeEach
	void setUp() {
		REQUESTS.clear();
	}

	@AfterAll
	static void stopTelegramApi() {
		TELEGRAM_API.stop(0);
	}

	@Test
	void sendReminder_shouldCallSendMessageWithInlineActions() throws InterruptedException {
		telegramClient.sendReminder(
				1001L,
				"Покормить кота",
				new TelegramReminderActions(
						"reminder:complete:00000000-0000-0000-0000-000000000001",
						"reminder:snooze:00000000-0000-0000-0000-000000000002",
						"reminder:cancel:00000000-0000-0000-0000-000000000001"));

		var request = nextRequest();
		assertThat(request.path()).isEqualTo("/bottest-token/sendMessage");
		assertThat(request.body())
				.contains("\"chat_id\":1001")
				.contains("\"text\":\"Покормить кота\"")
				.contains("\"inline_keyboard\"")
				.contains("\"text\":\"Готово\"")
				.contains("\"callback_data\":\"reminder:complete:00000000-0000-0000-0000-000000000001\"")
				.contains("\"text\":\"Отложить\"")
				.contains("\"callback_data\":\"reminder:snooze:00000000-0000-0000-0000-000000000002\"")
				.contains("\"text\":\"Отменить\"")
				.contains("\"callback_data\":\"reminder:cancel:00000000-0000-0000-0000-000000000001\"");
	}

	@Test
	void downloadFile_shouldResolveFilePathAndDownloadBytes() throws InterruptedException {
		var audio = telegramClient.downloadFile("voice-file-id");

		assertThat(audio).isEqualTo(VOICE_AUDIO);
		assertThat(nextRequest())
				.isEqualTo(new RecordedRequest(
						"/bottest-token/getFile",
						"{\"file_id\":\"voice-file-id\"}"));
		assertThat(nextRequest().path())
				.isEqualTo("/file/bottest-token/voice/reminder.oga");
	}

	@Test
	void answerCallbackQuery_shouldAcknowledgePressedButton() throws InterruptedException {
		telegramClient.answerCallbackQuery("callback-query-id");

		assertThat(nextRequest())
				.isEqualTo(new RecordedRequest(
						"/bottest-token/answerCallbackQuery",
						"{\"callback_query_id\":\"callback-query-id\"}"));
	}

	@Test
	void downloadFile_whenTelegramReturnsMalformedPath_shouldNotExposeBotToken() {
		assertThatThrownBy(() -> telegramClient.downloadFile("malformed-file-id"))
				.isInstanceOf(TelegramApiException.class)
				.hasMessage("Telegram Bot API request failed")
				.hasMessageNotContaining("test-token");
	}

	private RecordedRequest nextRequest() throws InterruptedException {
		var request = REQUESTS.poll(5, TimeUnit.SECONDS);
		assertThat(request).as("Telegram API request").isNotNull();
		return request;
	}

	private static HttpServer startTelegramApi() {
		try {
			var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/", TelegramBotApiClientIntegrationTest::handleRequest);
			server.start();
			return server;
		}
		catch (IOException exception) {
			throw new IllegalStateException("Failed to start Telegram API test server", exception);
		}
	}

	private static void handleRequest(HttpExchange exchange) throws IOException {
		var path = exchange.getRequestURI().getPath();
		var requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		REQUESTS.add(new RecordedRequest(path, requestBody));

		if (path.endsWith("/getFile")) {
			var filePath = requestBody.contains("malformed-file-id")
					? "http://[invalid"
					: "voice/reminder.oga";
			respondJson(exchange, "{\"ok\":true,\"result\":{\"file_path\":\"" + filePath + "\"}}");
			return;
		}
		if (path.startsWith("/file/")) {
			respond(exchange, "audio/ogg", VOICE_AUDIO);
			return;
		}
		respondJson(exchange, "{\"ok\":true,\"result\":{\"message_id\":42}}");
	}

	private static void respondJson(HttpExchange exchange, String body) throws IOException {
		respond(exchange, "application/json", body.getBytes(StandardCharsets.UTF_8));
	}

	private static void respond(HttpExchange exchange, String contentType, byte[] body) throws IOException {
		exchange.getResponseHeaders().add("Content-Type", contentType);
		exchange.sendResponseHeaders(200, body.length);
		try (var responseBody = exchange.getResponseBody()) {
			responseBody.write(body);
		}
	}

	private record RecordedRequest(String path, String body) {
	}
}
