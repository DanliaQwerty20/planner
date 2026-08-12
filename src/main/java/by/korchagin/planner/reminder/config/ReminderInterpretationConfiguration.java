package by.korchagin.planner.reminder.config;

import by.korchagin.planner.reminder.service.ReminderTextInterpreter;
import by.korchagin.planner.reminder.service.RussianReminderTextInterpreter;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ReminderInterpretationProperties.class)
public class ReminderInterpretationConfiguration {

	@Bean
	ReminderTextInterpreter reminderTextInterpreter(
			Clock clock,
			ReminderInterpretationProperties properties) {
		return new RussianReminderTextInterpreter(clock, properties.zoneId());
	}
}
