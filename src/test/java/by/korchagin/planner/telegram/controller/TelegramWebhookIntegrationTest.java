package by.korchagin.planner.telegram.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import by.korchagin.planner.telegram.service.TelegramUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"planner.telegram.enabled=true",
				"planner.telegram.bot-token=test-token",
				"planner.telegram.webhook-secret=test-webhook-secret"
		})
@Import(TestcontainersConfiguration.class)
class TelegramWebhookIntegrationTest {

	private static final String WEBHOOK_PATH = "/api/telegram/webhook";
	private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

	@LocalServerPort
	private int port;

	@MockitoBean
	private TelegramUpdateService telegramUpdateService;

	private RestTestClient restTestClient;

	@BeforeEach
	void setUp() {
		restTestClient = RestTestClient.bindToServer()
				.baseUrl("http://localhost:" + port)
				.build();
	}

	@Test
	void receiveUpdate_withValidSecret_shouldHandleUpdateAndReturnNoContent() {
		var update = textUpdate();

		restTestClient.post()
				.uri(WEBHOOK_PATH)
				.header(SECRET_HEADER, "test-webhook-secret")
				.body(update)
				.exchange()
				.expectStatus().isNoContent();

		verify(telegramUpdateService).handle(update);
	}

	@Test
	void receiveUpdate_withInvalidSecret_shouldReturnUnauthorizedWithoutHandlingUpdate() {
		var update = textUpdate();

		restTestClient.post()
				.uri(WEBHOOK_PATH)
				.header(SECRET_HEADER, "wrong-secret")
				.body(update)
				.exchange()
				.expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);

		verify(telegramUpdateService, never()).handle(update);
	}

	private TelegramUpdate textUpdate() {
		var user = new TelegramUpdate.TelegramUser(1001L);
		var chat = new TelegramUpdate.TelegramChat(1001L);
		var message = new TelegramUpdate.TelegramMessage(10L, user, chat, "завтра в 15:00 покормить кота", null);
		return new TelegramUpdate(20L, message, null);
	}
}
