package by.korchagin.planner.reminder.config;

import by.korchagin.planner.reminder.service.ReminderTextInterpreter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReminderInterpretationConfiguration {

	@Bean
	@ConditionalOnMissingBean(ReminderTextInterpreter.class)
	ReminderTextInterpreter reminderTextInterpreter() {
		return text -> {
			throw new IllegalStateException("Reminder text interpreter is not configured");
		};
	}
}
