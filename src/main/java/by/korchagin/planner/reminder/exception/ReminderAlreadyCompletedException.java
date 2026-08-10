package by.korchagin.planner.reminder.exception;

public class ReminderAlreadyCompletedException extends RuntimeException {

	public ReminderAlreadyCompletedException(String message) {
		super(message);
	}
}
