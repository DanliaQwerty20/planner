package by.korchagin.planner.telegram.controller;

import by.korchagin.planner.telegram.dto.TelegramUpdate;
import by.korchagin.planner.telegram.service.TelegramWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/telegram/webhook")
@ConditionalOnProperty(prefix = "planner.telegram", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "planner.telegram", name = "update-mode", havingValue = "webhook")
public class TelegramWebhookController {

	private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

	private final TelegramWebhookService telegramWebhookService;

	@PostMapping
	public ResponseEntity<Void> receiveUpdate(
			@RequestHeader(name = SECRET_HEADER, required = false) String secret,
			@RequestBody TelegramUpdate update) {
		if (!telegramWebhookService.handle(secret, update)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		return ResponseEntity.noContent().build();
	}
}
