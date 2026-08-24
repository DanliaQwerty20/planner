package by.korchagin.planner.telegram.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramDeleteWebhookRequest(
		@JsonProperty("drop_pending_updates") boolean dropPendingUpdates) {
}
