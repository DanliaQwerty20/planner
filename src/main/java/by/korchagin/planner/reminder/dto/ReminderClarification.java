package by.korchagin.planner.reminder.dto;

public record ReminderClarification(
		String question,
		boolean retainContext) implements ReminderInterpretationResult {

	public ReminderClarification(String question) {
		this(question, true);
	}
}
