package main.ui;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * MusicPlayer – Singleton audio engine.
 *
 * FIXES applied (based on Claude's v2 complete rewrite):
 * 1. Uses a dedicated lock object to avoid deadlocks with Swing.
 * 2. Clip is opened once and re-used; start()/stop() are the only calls after open.
 * 3. loadTrack() closes the old clip before opening a new one atomically.
 * 4. All state mutations are synchronized on the same lock, so toggleMute() is
 *    always consistent, even when called from different threads.
 * 5. No volatile or ready flag – visibility is guaranteed by the lock.
 */
public class MusicPlayer {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static volatile MusicPlayer instance;

    public static MusicPlayer getInstance(String defaultTrackPath) {
        if (instance == null) {
            synchronized (MusicPlayer.class) {
                if (instance == null) {
                    instance = new MusicPlayer(defaultTrackPath);
                }
            }
        }
        return instance;
    }

    public static MusicPlayer getInstance() {
        return getInstance("src/resources/sounds/background.wav");
    }

    // ── Internal lock (separate from 'this' to avoid deadlock with Swing) ─────
    private final Object lock = new Object();

    // ── State ─────────────────────────────────────────────────────────────────
    private Clip    clip;
    private boolean muted       = false;
    private String  currentPath;

    // ── Construction ──────────────────────────────────────────────────────────
    private MusicPlayer(String trackPath) {
        this.currentPath = trackPath;
        loadAndPlay(trackPath);
    }

    // ── Core loader ───────────────────────────────────────────────────────────

    /**
     * Loads an audio file on a background thread.
     * Closes any previously playing clip first so we never leak a Line.
     */
    private void loadAndPlay(String path) {
        Thread t = new Thread(() -> {
            synchronized (lock) {
                // 1. Tear down previous clip cleanly
                if (clip != null) {
                    try {
                        if (clip.isRunning()) clip.stop();
                        clip.close();
                    } catch (Exception ignore) {}
                    clip = null;
                }

                // 2. Load new file
                File f = new File(path);
                if (!f.exists()) {
                    System.err.println("[MusicPlayer] File not found: " + path);
                    return;
                }

                try {
                    AudioInputStream raw = AudioSystem.getAudioInputStream(f);

                    // Convert to PCM if needed (e.g. MP3 via SPI)
                    AudioFormat base = raw.getFormat();
                    AudioFormat pcm  = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        base.getSampleRate(),
                        16,
                        base.getChannels(),
                        base.getChannels() * 2,
                        base.getSampleRate(),
                        false
                    );
                    AudioInputStream pcmStream =
                        AudioSystem.isConversionSupported(pcm, base)
                            ? AudioSystem.getAudioInputStream(pcm, raw)
                            : raw;

                    Clip newClip = AudioSystem.getClip();
                    newClip.open(pcmStream);

                    // 3. Assign and start (respecting current mute state)
                    clip = newClip;
                    clip.loop(Clip.LOOP_CONTINUOUSLY);   // arms looping
                    if (!muted) {
                        clip.start();                    // begin playback
                    }
                    // If muted: clip is armed but not started – toggleMute()
                    // will call clip.start() when the user un-mutes.

                } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
                    System.err.println("[MusicPlayer] Load failed '" + path + "': " + e.getMessage());
                }
            }
        }, "MusicPlayer-Loader");
        t.setDaemon(true);
        t.start();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Immediate mute / unmute toggle – no lag.
     *
     * Uses start()/stop() on the already-loaded Clip rather than
     * re-opening anything, so the call returns in microseconds.
     */
    public void toggleMute() {
        synchronized (lock) {
            muted = !muted;
            if (clip == null) return;          // still loading – flag recorded, applied when ready
            try {
                if (muted) {
                    if (clip.isRunning()) clip.stop();
                } else {
                    if (!clip.isRunning()) clip.start();
                }
            } catch (Exception e) {
                System.err.println("[MusicPlayer] Toggle error: " + e.getMessage());
            }
        }
    }

    /** Sync mute state without toggling – used when panels share the singleton. */
    public void setMuted(boolean mute) {
        synchronized (lock) {
            if (muted == mute) return;
            muted = mute;
            if (clip == null) return;
            try {
                if (muted && clip.isRunning())  clip.stop();
                if (!muted && !clip.isRunning()) clip.start();
            } catch (Exception e) {
                System.err.println("[MusicPlayer] setMuted error: " + e.getMessage());
            }
        }
    }

    public boolean isMuted() {
        synchronized (lock) { return muted; }
    }

    /**
     * Switches to a new audio track at runtime.
     * No-op if the requested path is already loaded.
     */
    public void loadTrack(String path) {
        if (path == null) return;
        synchronized (lock) {
            if (path.equals(currentPath) && clip != null) return;
            currentPath = path;
        }
        // Load outside the lock so we don't block the EDT
        loadAndPlay(path);
    }

    public String getCurrentTrackPath() {
        synchronized (lock) { return currentPath; }
    }
}