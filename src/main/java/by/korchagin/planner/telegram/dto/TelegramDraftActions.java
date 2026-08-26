package by.korchagin.planner.telegram.dto;

public record TelegramDraftActions(
		String confirmationData,
		String editingData,
		String cancellationData) {
}
