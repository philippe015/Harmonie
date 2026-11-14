package fr.byphilou.harmonie;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.audiofx.Visualizer;
import android.os.SystemClock;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Only visualizes the Media3 audio session, never the device microphone nor global mix.
 * This app-private snapshot is read by the WebView bridge without touching MediaController.
 * FFT and waveform are MONO and deliberately low-resolution (Android Visualizer contract).
 */
final class NativeBeatVisualizer {
    static final String PREFS = "harmonie_native_led_v3517";
    static final String ACTION_UPDATE = "fr.byphilou.harmonie.action.UPDATE_LED_CAPTURE";
    static volatile String latestFrame = "{\"status\":\"disabled\",\"ready\":false}";
    private final Context context;
    private volatile Visualizer visualizer;
    private volatile int sessionId = -1;
    private int attachedId = -1;
    private boolean visible = false;
    private boolean enabled = false;
    private boolean playing = false;
    private volatile float monoRms = 0;
    private volatile float bassAverage = 0.13f;
    private volatile long beatAt = 0;
    private volatile float pulse = 0;

    NativeBeatVisualizer(Context context) { this.context = context.getApplicationContext(); }
    static String getFrame() { return latestFrame; }
    void configure(boolean requested, boolean isVisible) {
        enabled = requested;
        visible = isVisible;
        refresh();
    }
    void onSessionChanged(int id) {
        if (sessionId != id) { sessionId = id; refresh(); }
    }
    void onPlayingChanged(boolean isPlaying) {
        if (playing != isPlaying) { playing = isPlaying; refresh(); }
    }
    private void status(String reason) {
        latestFrame = "{\"status\":\"" + reason + "\",\"ready\":false}";
    }
    private void close() {
        if (visualizer != null) {
            Visualizer old = visualizer;
            visualizer = null;
            try { old.setEnabled(false); } catch (Exception ignored) { }
            try { old.release(); } catch (Exception ignored) { }
        }
        attachedId = -1;
    }
    void release() { close(); status("disabled"); }
    private void refresh() {
        if (!enabled) { close(); status("disabled"); return; }
        if (!visible) { close(); status("background"); return; }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            close(); status("permission"); return;
        }
        if (!playing) { close(); status("paused"); return; }
        if (sessionId <= 0) { close(); status("session"); return; }
        if (visualizer != null && attachedId == sessionId) return;
        close();
        try {
            Visualizer v = new Visualizer(sessionId); // NEVER use session 0 (global output capture).
            try {
                int[] range = Visualizer.getCaptureSizeRange();
                int size = Math.min(1024, range[1]);
                while (size > range[0] && (size & (size-1)) != 0) size--;
                if (size >= range[0] && size <= range[1]) v.setCaptureSize(size);
                int rate = Math.min(20000, Visualizer.getMaxCaptureRate()); // 20 Hz in milliHertz.
                int result = v.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                    @Override public void onWaveFormDataCapture(Visualizer source, byte[] data, int sampleRate) {
                        if (source != visualizer || data == null || data.length == 0) return;
                        double sq = 0;
                        for (byte sample : data) {
                            double x = ((sample & 0xff) - 128) / 128.0;
                            sq += x * x;
                        }
                        monoRms = (float) Math.min(1.0, Math.sqrt(sq / data.length) * 3.4);
                    }
                    @Override public void onFftDataCapture(Visualizer source, byte[] data, int sampleRate) {
                        if (source != visualizer || data == null || data.length < 64) return;
                        publishFft(data, sampleRate);
                    }
                }, rate, true, true);
                if (result != Visualizer.SUCCESS) throw new IllegalStateException("Visualizer callback=" + result);
                visualizer = v;
                attachedId = sessionId;
                status("waiting");
                v.setEnabled(true);
            } catch (Exception ex) {
                try { v.release(); } catch (Exception ignored) { }
                throw ex;
            }
        } catch (Exception | LinkageError ex) {
            Log.w("HarmonieLED", "Capture FFT non disponible pour cette session", ex);
            close(); status("unsupported");
        }
    }
    private void publishFft(byte[] fft, int samplingRateMilliHz) {
        // Android Visualizer reports the sample rate in milliHz, and captures only mono FFT.
        double hz = samplingRateMilliHz > 0 ? samplingRateMilliHz / 1000.0 : 48000.0;
        int n = fft.length;
        double binHz = hz / n;
        JSONArray levels = new JSONArray();
        double bass = 0, total = 0;
        for (int i = 0; i < 30; i++) {
            double f0 = 50 * Math.pow(16000.0 / 50, i / 30.0);
            double f1 = 50 * Math.pow(16000.0 / 50, (i + 1.0) / 30.0);
            int a = Math.max(1, Math.min(n / 2 - 1, (int)Math.floor(f0 / binHz)));
            int b = Math.max(a + 1, Math.min(n / 2, (int)Math.ceil(f1 / binHz)));
            double sum = 0;
            for (int k = a; k < b; k++) {
                int re = fft[2*k], im = fft[2*k+1];
                sum += Math.hypot(re, im);
            }
            double magnitude = sum / (b - a);
            double level = Math.max(0, Math.min(1, (magnitude - 2.0) / 48.0));
            level = Math.pow(level, 0.74);
            levels.put(Double.valueOf(Math.round(level * 1000.0) / 1000.0));
            if (i < 7) bass += level;
            total += level;
        }
        float bassLevel = (float)(bass / 7);
        bassAverage = bassAverage * 0.92f + bassLevel * 0.08f;
        long now = SystemClock.elapsedRealtime();
        boolean beat = bassLevel > Math.max(0.14f, bassAverage * 1.48f) && now - beatAt > 190;
        if (beat) { beatAt = now; pulse = 1f; }
        else pulse *= 0.81f;
        try {
            JSONObject frame = new JSONObject();
            frame.put("status", "capture");
            frame.put("ready", true);
            frame.put("timestamp", System.currentTimeMillis());
            frame.put("sessionId", sessionId);
            frame.put("mono", true);
            frame.put("levels", levels);
            frame.put("rms", Math.round(monoRms * 1000) / 1000.0);
            frame.put("energy", Math.round(total / 30 * 1000) / 1000.0);
            frame.put("beat", beat);
            frame.put("pulse", Math.round(pulse * 1000) / 1000.0);
            latestFrame = frame.toString();
        } catch (Exception ignored) { }
    }
}
