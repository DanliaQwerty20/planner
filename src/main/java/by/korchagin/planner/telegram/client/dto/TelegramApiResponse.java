package by.korchagin.planner.telegram.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramApiResponse(boolean ok, String description) {
}
