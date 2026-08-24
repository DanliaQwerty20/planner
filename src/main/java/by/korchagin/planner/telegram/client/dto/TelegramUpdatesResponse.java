package by.korchagin.planner.telegram.client.dto;

import java.util.List;

import by.korchagin.planner.telegram.dto.TelegramUpdate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdatesResponse(
		boolean ok,
		List<TelegramUpdate> result,
		String description) {
}
