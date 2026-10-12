package com.quizmaster.services;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * Plays short sound effects for correct/wrong answers and pass/fail results.
 *
 * Tones are SYNTHESIZED at runtime with the Java Sound API (javax.sound.sampled)
 * rather than loaded from .wav files. This avoids any resource-path or packaging
 * issues — there are no binary assets to ship — and works on any machine with an
 * audio output device. If no audio device is available, playback fails silently
 * and never interrupts the quiz. Each sound plays on its own daemon thread.
 */
public class SoundService {

    private static final float SAMPLE_RATE = 44_100f;
    private static boolean enabled = true;

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }

    /** Pleasant rising two-note chime for a correct answer. */
    public static void playCorrect() {
        playSequenceAsync(new Tone[]{
                new Tone(659.25, 110),  // E5
                new Tone(987.77, 170)   // B5
        }, 0.5);
    }

    /** Low descending buzz for a wrong answer. */
    public static void playWrong() {
        playSequenceAsync(new Tone[]{
                new Tone(233.08, 160),  // Bb3
                new Tone(174.61, 260)   // F3
        }, 0.45);
    }

    /** Triumphant ascending arpeggio (C major) for passing a quiz. */
    public static void playPass() {
        playSequenceAsync(new Tone[]{
                new Tone(523.25, 120),  // C5
                new Tone(659.25, 120),  // E5
                new Tone(783.99, 120),  // G5
                new Tone(1046.50, 240)  // C6
        }, 0.5);
    }

    /** Downcast descending tones for failing a quiz. */
    public static void playFail() {
        playSequenceAsync(new Tone[]{
                new Tone(392.00, 180),  // G4
                new Tone(311.13, 200),  // Eb4
                new Tone(261.63, 320)   // C4
        }, 0.45);
    }

    /** A single tone: frequency in Hz and duration in milliseconds. */
    private record Tone(double freq, int millis) {}

    private static void playSequenceAsync(Tone[] tones, double volume) {
        if (!enabled) return;
        Thread t = new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) return;
                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    line.open(format);
                    line.start();
                    for (Tone tone : tones) {
                        byte[] buffer = synthesize(tone.freq(), tone.millis(), volume);
                        line.write(buffer, 0, buffer.length);
                    }
                    line.drain();
                }
            } catch (Exception ignored) {
                // No audio device / line unavailable — fail silently.
            }
        }, "sound-fx");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Generates 16-bit mono PCM samples for a sine tone, with a short linear
     * fade in/out envelope to avoid audible clicks at the start/end.
     */
    private static byte[] synthesize(double freq, int millis, double volume) {
        int samples = (int) (SAMPLE_RATE * millis / 1000.0);
        byte[] out = new byte[samples * 2]; // 2 bytes per 16-bit sample
        int fade = (int) (SAMPLE_RATE * 0.008); // 8 ms fade
        for (int i = 0; i < samples; i++) {
            double angle = 2.0 * Math.PI * i * freq / SAMPLE_RATE;
            double envelope = 1.0;
            if (i < fade) {
                envelope = (double) i / fade;
            } else if (i > samples - fade) {
                envelope = (double) (samples - i) / fade;
            }
            short value = (short) (Math.sin(angle) * volume * envelope * Short.MAX_VALUE);
            out[i * 2] = (byte) (value & 0xff);
            out[i * 2 + 1] = (byte) ((value >> 8) & 0xff);
        }
        return out;
    }
}
