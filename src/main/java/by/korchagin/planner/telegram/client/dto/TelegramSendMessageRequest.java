package by.korchagin.planner.telegram.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramSendMessageRequest(
		@JsonProperty("chat_id") long chatId,
		String text,
		@JsonProperty("reply_markup") InlineKeyboardMarkup replyMarkup) {

	public record InlineKeyboardMarkup(
			@JsonProperty("inline_keyboard") List<List<InlineKeyboardButton>> inlineKeyboard) {
	}

	public record InlineKeyboardButton(
			String text,
			@JsonProperty("callback_data") String callbackData) {
	}
}
