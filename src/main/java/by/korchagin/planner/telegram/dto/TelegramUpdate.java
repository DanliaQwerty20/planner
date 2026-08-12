package by.korchagin.planner.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdate(
		@JsonProperty("update_id") long updateId,
		TelegramMessage message,
		@JsonProperty("callback_query") TelegramCallbackQuery callbackQuery) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TelegramMessage(
			@JsonProperty("message_id") long messageId,
			TelegramUser from,
			TelegramChat chat,
			String text,
			TelegramVoice voice) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TelegramCallbackQuery(
			String id,
			TelegramUser from,
			TelegramMessage message,
			String data) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TelegramUser(long id) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TelegramVoice(
			@JsonProperty("file_id") String fileId,
			@JsonProperty("file_unique_id") String fileUniqueId,
			int duration,
			@JsonProperty("mime_type") String mimeType,
			@JsonProperty("file_size") long fileSize) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TelegramChat(long id) {
	}
}
