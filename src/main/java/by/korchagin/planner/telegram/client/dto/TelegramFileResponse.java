package by.korchagin.planner.telegram.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramFileResponse(boolean ok, TelegramFile result, String description) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TelegramFile(@JsonProperty("file_path") String filePath) {
	}
}
