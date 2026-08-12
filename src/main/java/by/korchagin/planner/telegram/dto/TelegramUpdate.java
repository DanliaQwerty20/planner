package by.korchagin.planner.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdate(
		@JsonProperty("update_id") long updateId,
		TelegramMessage message) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TelegramMessage(
			@JsonProperty("message_id") long messageId,
			TelegramChat chat,
			String text) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TelegramChat(long id) {
	}
}
