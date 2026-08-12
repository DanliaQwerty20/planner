package by.korchagin.planner.reminder.service;

import by.korchagin.planner.reminder.dto.ReminderInterpretationResult;

public interface ReminderTextInterpreter {

	ReminderInterpretationResult interpret(String text);
}
