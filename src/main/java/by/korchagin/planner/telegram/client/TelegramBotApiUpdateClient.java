package by.korchagin.planner.telegram.client;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import by.korchagin.planner.telegram.client.dto.TelegramApiResponse;
import by.korchagin.planner.telegram.client.dto.TelegramDeleteWebhookRequest;
import by.korchagin.planner.telegram.client.dto.TelegramGetUpdatesRequest;
import by.korchagin.planner.telegram.client.dto.TelegramUpdatesResponse;
import by.korchagin.planner.telegram.config.TelegramProperties;
import by.korchagin.planner.telegram.dto.TelegramUpdate;
import by.korchagin.planner.telegram.exception.TelegramApiException;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class TelegramBotApiUpdateClient implements TelegramUpdateClient {

	private static final int UPDATE_LIMIT = 100;
	private static final List<String> ALLOWED_UPDATES = List.of("message", "callback_query");

	private final RestClient restClient;
	private final String botApiBaseUrl;

	public TelegramBotApiUpdateClient(RestClient.Builder restClientBuilder, TelegramProperties telegramProperties) {
		this.restClient = restClientBuilder.build();
		var apiBaseUrl = withoutTrailingSlash(telegramProperties.apiBaseUrl());
		this.botApiBaseUrl = apiBaseUrl + "/bot" + telegramProperties.botToken() + "/";
	}

	@Override
	public void deleteWebhook(boolean dropPendingUpdates) {
		var response = post(
				"deleteWebhook",
				new TelegramDeleteWebhookRequest(dropPendingUpdates),
				TelegramApiResponse.class);
		if (response == null || !response.ok()) {
			throw apiFailure(response == null ? null : response.description());
		}
	}

	@Override
	public List<TelegramUpdate> getUpdates(Long offset, Duration timeout) {
		var request = new TelegramGetUpdatesRequest(
				offset,
				UPDATE_LIMIT,
				timeout.toSeconds(),
				ALLOWED_UPDATES);
		var response = post("getUpdates", request, TelegramUpdatesResponse.class);
		if (response == null || !response.ok()) {
			throw apiFailure(response == null ? null : response.description());
		}
		return response.result() == null ? List.of() : response.result();
	}

	private <T> T post(String method, Object request, Class<T> responseType) {
		try {
			return restClient.post()
					.uri(URI.create(botApiBaseUrl + method))
					.contentType(MediaType.APPLICATION_JSON)
					.body(request)
					.retrieve()
					.body(responseType);
		}
		catch (RestClientException | IllegalArgumentException exception) {
			throw apiFailure(null);
		}
	}

	private TelegramApiException apiFailure(String description) {
		if (description == null || description.isBlank()) {
			return new TelegramApiException("Telegram Bot API request failed");
		}
		return new TelegramApiException("Telegram Bot API rejected request: " + description);
	}

	private static String withoutTrailingSlash(URI uri) {
		var value = uri.toString();
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}
}
