package by.korchagin.planner.voice.service;

public interface SpeechTranscriber {

	boolean isAvailable();

	String transcribe(byte[] audio);
}
