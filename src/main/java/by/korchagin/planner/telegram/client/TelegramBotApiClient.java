package by.korchagin.planner.telegram.client;

import java.net.URI;
import java.util.List;

import by.korchagin.planner.telegram.client.dto.TelegramApiResponse;
import by.korchagin.planner.telegram.client.dto.TelegramAnswerCallbackQueryRequest;
import by.korchagin.planner.telegram.client.dto.TelegramFileResponse;
import by.korchagin.planner.telegram.client.dto.TelegramGetFileRequest;
import by.korchagin.planner.telegram.client.dto.TelegramSendMessageRequest;
import by.korchagin.planner.telegram.client.dto.TelegramSendMessageRequest.InlineKeyboardButton;
import by.korchagin.planner.telegram.client.dto.TelegramSendMessageRequest.InlineKeyboardMarkup;
import by.korchagin.planner.telegram.config.TelegramProperties;
import by.korchagin.planner.telegram.dto.TelegramDraftActions;
import by.korchagin.planner.telegram.dto.TelegramReminderActions;
import by.korchagin.planner.telegram.exception.TelegramApiException;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class TelegramBotApiClient implements TelegramClient {

	private static final String SEND_MESSAGE_METHOD = "sendMessage";
	private static final String GET_FILE_METHOD = "getFile";
	private static final String ANSWER_CALLBACK_QUERY_METHOD = "answerCallbackQuery";

	private final RestClient restClient;
	private final String botApiBaseUrl;
	private final String fileApiBaseUrl;

	public TelegramBotApiClient(RestClient.Builder restClientBuilder, TelegramProperties telegramProperties) {
		this.restClient = restClientBuilder.build();
		var apiBaseUrl = withoutTrailingSlash(telegramProperties.apiBaseUrl());
		this.botApiBaseUrl = apiBaseUrl + "/bot" + telegramProperties.botToken() + "/";
		this.fileApiBaseUrl = apiBaseUrl + "/file/bot" + telegramProperties.botToken() + "/";
	}

	@Override
	public void sendMessage(long chatId, String text) {
		sendMessage(new TelegramSendMessageRequest(chatId, text, null));
	}

	@Override
	public void sendReminder(long chatId, String text, TelegramReminderActions actions) {
		var keyboard = new InlineKeyboardMarkup(List.of(
				List.of(
						new InlineKeyboardButton("Готово", actions.completionData()),
						new InlineKeyboardButton("Отложить", actions.snoozeData())),
				List.of(new InlineKeyboardButton("Отменить", actions.cancellationData()))));
		sendMessage(new TelegramSendMessageRequest(chatId, text, keyboard));
	}

	@Override
	public void sendConfirmation(long chatId, String text, TelegramDraftActions actions) {
		var keyboard = new InlineKeyboardMarkup(List.of(
				List.of(new InlineKeyboardButton("Подтвердить", actions.confirmationData())),
				List.of(
						new InlineKeyboardButton("Исправить", actions.editingData()),
						new InlineKeyboardButton("Отменить", actions.cancellationData()))));
		sendMessage(new TelegramSendMessageRequest(chatId, text, keyboard));
	}

	@Override
	public void answerCallbackQuery(String callbackQueryId) {
		var response = post(
				ANSWER_CALLBACK_QUERY_METHOD,
				new TelegramAnswerCallbackQueryRequest(callbackQueryId),
				TelegramApiResponse.class);
		if (response == null || !response.ok()) {
			throw apiFailure(response == null ? null : response.description());
		}
	}

	@Override
	public byte[] downloadFile(String fileId) {
		var response = post(
				GET_FILE_METHOD,
				new TelegramGetFileRequest(fileId),
				TelegramFileResponse.class);
		if (response == null || !response.ok() || response.result() == null
				|| response.result().filePath() == null || response.result().filePath().isBlank()) {
			throw apiFailure(response == null ? null : response.description());
		}

		try {
			return restClient.get()
					.uri(URI.create(fileApiBaseUrl + response.result().filePath()))
					.retrieve()
					.body(byte[].class);
		}
		catch (RestClientException | IllegalArgumentException exception) {
			throw apiFailure(null);
		}
	}

	private void sendMessage(TelegramSendMessageRequest request) {
		var response = post(SEND_MESSAGE_METHOD, request, TelegramApiResponse.class);
		if (response == null || !response.ok()) {
			throw apiFailure(response == null ? null : response.description());
		}
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
