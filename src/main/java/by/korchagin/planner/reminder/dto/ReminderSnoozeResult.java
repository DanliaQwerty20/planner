package by.korchagin.planner.reminder.dto;

import java.time.Instant;

public record ReminderSnoozeResult(String text, Instant snoozedUntil) {
}
