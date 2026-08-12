package by.korchagin.planner.reminder.service;

import by.korchagin.planner.reminder.dto.ReminderInterpretation;

public interface ReminderTextInterpreter {

	ReminderInterpretation interpret(String text);
}
