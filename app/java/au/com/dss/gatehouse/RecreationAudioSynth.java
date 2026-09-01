package au.com.dss.gatehouse;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.concurrent.Executors;

/**
 * RecreationAudioSynth — Zero-dependency Procedural Audio Synthesizer.
 * Generates acoustic resonances for Baduk stones clacking on Hon-Kaya wood,
 * weighted wooden Chess pieces thudding on felt-lined oak, and Ur pyramid dice rolls.
 */
public class RecreationAudioSynth {
    private static final int SAMPLE_RATE = 22050;

    public static void playBadukStoneClack() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 85;
                    int numSamples = (SAMPLE_RATE * durationMs) / 1000;
                    short[] buffer = new short[numSamples];

                    // Synthesize sharp crisp high-frequency transient (1950Hz) + fast decaying wood resonance (480Hz)
                    for (int i = 0; i < numSamples; i++) {
                        double t = (double) i / SAMPLE_RATE;
                        double decayFast = Math.exp(-t * 90.0);
                        double decayBody = Math.exp(-t * 35.0);

                        double click = Math.sin(2.0 * Math.PI * 1950.0 * t) * decayFast;
                        double body = Math.sin(2.0 * Math.PI * 480.0 * t + 0.3) * decayBody;
                        double snap = (Math.random() * 2.0 - 1.0) * Math.exp(-t * 220.0) * 0.4;

                        double sample = (click * 0.65 + body * 0.45 + snap * 0.25);
                        buffer[i] = (short) (Math.max(-1.0, Math.min(1.0, sample)) * 32000.0);
                    }

                    playPcmBuffer(buffer);
                } catch (Throwable ignored) {}
            }
        });
    }

    public static void playChessPieceThud(final boolean isCapture) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = isCapture ? 95 : 75;
                    int numSamples = (SAMPLE_RATE * durationMs) / 1000;
                    short[] buffer = new short[numSamples];

                    // Weighted wooden piece landing on felt-lined oak: low thud (220Hz) + piece strike
                    for (int i = 0; i < numSamples; i++) {
                        double t = (double) i / SAMPLE_RATE;
                        double decay = Math.exp(-t * (isCapture ? 45.0 : 60.0));
                        double strike = isCapture ? Math.sin(2.0 * Math.PI * 1100.0 * t) * Math.exp(-t * 120.0) * 0.6 : 0.0;
                        double thud = Math.sin(2.0 * Math.PI * 220.0 * t) * decay * 0.75;
                        double felt = Math.sin(2.0 * Math.PI * 110.0 * t) * decay * 0.35;

                        double sample = (thud + felt + strike);
                        buffer[i] = (short) (Math.max(-1.0, Math.min(1.0, sample)) * 30000.0);
                    }

                    playPcmBuffer(buffer);
                } catch (Throwable ignored) {}
            }
        });
    }

    public static void playDiceRoll() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int durationMs = 120;
                    int numSamples = (SAMPLE_RATE * durationMs) / 1000;
                    short[] buffer = new short[numSamples];

                    // Pyramid dice tumble clicks
                    for (int i = 0; i < numSamples; i++) {
                        double t = (double) i / SAMPLE_RATE;
                        double sample = (Math.random() * 2.0 - 1.0) * Math.exp(-t * 25.0) * 0.45;
                        if (i % 200 < 50) sample += Math.sin(2.0 * Math.PI * 850.0 * t) * 0.4;
                        buffer[i] = (short) (Math.max(-1.0, Math.min(1.0, sample)) * 26000.0);
                    }

                    playPcmBuffer(buffer);
                } catch (Throwable ignored) {}
            }
        });
    }

    private static void playPcmBuffer(short[] buffer) {
        try {
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(buffer.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();

            track.write(buffer, 0, buffer.length);
            track.play();
            // Automatically release when playback finishes
            track.setNotificationMarkerPosition(buffer.length);
            track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onMarkerReached(AudioTrack t) {
                    try {
                        t.stop();
                        t.release();
                    } catch (Throwable ignored) {}
                }
                @Override
                public void onPeriodicNotification(AudioTrack t) {}
            });
        } catch (Throwable ignored) {}
    }
}
