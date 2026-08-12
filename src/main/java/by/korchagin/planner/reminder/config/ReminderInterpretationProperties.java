package by.korchagin.planner.reminder.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "planner.reminder.interpretation")
public record ReminderInterpretationProperties(String timeZone) {

	public ReminderInterpretationProperties {
		if (timeZone == null || timeZone.isBlank()) {
			timeZone = "Europe/Moscow";
		}
	}

	public ZoneId zoneId() {
		return ZoneId.of(timeZone);
	}
}
