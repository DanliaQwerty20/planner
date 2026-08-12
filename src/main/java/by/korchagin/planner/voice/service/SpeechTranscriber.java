package by.korchagin.planner.voice.service;

public interface SpeechTranscriber {

	String transcribe(byte[] audio);
}
