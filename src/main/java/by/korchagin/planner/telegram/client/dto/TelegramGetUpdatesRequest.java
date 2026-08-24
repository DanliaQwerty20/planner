package by.korchagin.planner.telegram.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramGetUpdatesRequest(
		Long offset,
		int limit,
		long timeout,
		@JsonProperty("allowed_updates") List<String> allowedUpdates) {
}
