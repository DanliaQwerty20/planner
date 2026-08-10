package by.korchagin.planner.reminder.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import by.korchagin.planner.TestcontainersConfiguration;
import by.korchagin.planner.reminder.entity.Reminder;
import by.korchagin.planner.reminder.entity.ReminderStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ReminderRepositoryTest {

	private static final long TELEGRAM_USER_ID = 42L;
	@Autowired
	private ReminderRepository reminderRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void save_whenReminderIsNew_shouldPersistAndReloadIt() {
		var beforeSave = Instant.now().truncatedTo(ChronoUnit.MICROS);
		var remindAt = beforeSave.plusSeconds(3600);
		var reminder = Reminder.schedule(
				TELEGRAM_USER_ID,
				"Покормить кота",
				remindAt,
				beforeSave);

		var savedReminder = reminderRepository.saveAndFlush(reminder);
		var afterSave = Instant.now();
		entityManager.clear();
		var reloadedReminder = reminderRepository
				.findByIdAndTelegramUserId(savedReminder.getId(), TELEGRAM_USER_ID)
				.orElseThrow();

		assertThat(savedReminder).isSameAs(reminder);
		assertThat(savedReminder.getId()).isNotNull();
		assertThat(savedReminder.getVersion()).isNotNull();
		assertThat(reloadedReminder.getTelegramUserId()).isEqualTo(TELEGRAM_USER_ID);
		assertThat(reloadedReminder.getText()).isEqualTo("Покормить кота");
		assertThat(reloadedReminder.getRemindAt()).isEqualTo(remindAt);
		assertThat(reloadedReminder.getCreatedAt()).isBetween(beforeSave, afterSave);
		assertThat(reloadedReminder.getStatus()).isEqualTo(ReminderStatus.SCHEDULED);
	}
}
