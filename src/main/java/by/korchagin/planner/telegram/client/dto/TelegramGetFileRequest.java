package by.korchagin.planner.telegram.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramGetFileRequest(@JsonProperty("file_id") String fileId) {
}
