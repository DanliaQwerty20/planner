package by.korchagin.planner.telegram.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramAnswerCallbackQueryRequest(
		@JsonProperty("callback_query_id") String callbackQueryId) {
}
