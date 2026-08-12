package by.korchagin.planner.config;

import by.korchagin.planner.reminder.delivery.adapter.UnconfiguredReminderMessageSender;
import by.korchagin.planner.reminder.delivery.gateway.ReminderMessageSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReminderDeliveryConfiguration {

	@Bean
	@ConditionalOnMissingBean(ReminderMessageSender.class)
	ReminderMessageSender reminderMessageSender() {
		return new UnconfiguredReminderMessageSender();
	}
}
