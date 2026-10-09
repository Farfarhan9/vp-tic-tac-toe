package main.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ThemeManager – centralises application-wide colour themes (Feature 3e).
 *
 * Four built-in themes:  Light (default) · Dark · Blue · Green
 *
 * Any panel that wants to be theme-aware should:
 *   1. Call ThemeManager.getInstance().register(this)  in its constructor.
 *   2. Implement ThemeListener and apply colours inside onThemeChanged().
 *   3. Call ThemeManager.getInstance().unregister(this) when disposed.
 *
 * The manager pushes updates on the EDT so listeners never need to worry
 * about thread safety for Swing operations.
 */
public class ThemeManager {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static ThemeManager instance;
    public static synchronized ThemeManager getInstance() {
        if (instance == null) instance = new ThemeManager();
        return instance;
    }

    // ── Theme definitions ─────────────────────────────────────────────────────
    public enum Theme { LIGHT, DARK, BLUE, GREEN }

    public static class ThemeColors {
        public final Color background;
        public final Color cardBackground;
        public final Color cardBorder;
        public final Color textMain;
        public final Color textMuted;
        public final Color toggleOn;
        public final Color buttonBackground;
        public final Color buttonText;
        public final Color accent;

        ThemeColors(Color bg, Color cardBg, Color cardBorder,
                    Color textMain, Color textMuted, Color toggleOn,
                    Color btnBg, Color btnText, Color accent) {
            this.background      = bg;
            this.cardBackground  = cardBg;
            this.cardBorder      = cardBorder;
            this.textMain        = textMain;
            this.textMuted       = textMuted;
            this.toggleOn        = toggleOn;
            this.buttonBackground = btnBg;
            this.buttonText      = btnText;
            this.accent          = accent;
        }
    }

    // Pre-built palettes
    public static final ThemeColors LIGHT = new ThemeColors(
        new Color(245,245,245), Color.WHITE, new Color(230,230,230),
        new Color(30,30,30),    new Color(120,120,120), new Color(24,107,121),
        Color.WHITE,            new Color(40,40,40),    new Color(0,128,128)
    );

    public static final ThemeColors DARK = new ThemeColors(
        new Color(28,28,30),    new Color(44,44,46),    new Color(58,58,60),
        new Color(242,242,247), new Color(174,174,178), new Color(48,209,88),
        new Color(44,44,46),    new Color(242,242,247), new Color(10,132,255)
    );

    public static final ThemeColors BLUE = new ThemeColors(
        new Color(224,237,250), new Color(255,255,255), new Color(190,220,245),
        new Color(10,50,100),   new Color(80,120,170),  new Color(0,102,204),
        Color.WHITE,            new Color(10,50,100),   new Color(0,102,204)
    );

    public static final ThemeColors GREEN = new ThemeColors(
        new Color(230,245,234), new Color(255,255,255), new Color(190,230,200),
        new Color(10,60,30),    new Color(70,130,90),   new Color(34,139,34),
        Color.WHITE,            new Color(10,60,30),    new Color(34,139,34)
    );

    // ── Listener interface ────────────────────────────────────────────────────
    public interface ThemeListener {
        void onThemeChanged(ThemeColors colors);
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private Theme currentTheme = Theme.LIGHT;
    private final List<ThemeListener> listeners = new ArrayList<>();

    // ── Registration ──────────────────────────────────────────────────────────
    public synchronized void register(ThemeListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public synchronized void unregister(ThemeListener l) {
        listeners.remove(l);
    }

    // ── Apply ─────────────────────────────────────────────────────────────────
    public void applyTheme(Theme theme) {
        currentTheme = theme;
        ThemeColors colors = getColors(theme);
        SwingUtilities.invokeLater(() -> {
            synchronized (this) {
                for (ThemeListener l : listeners) {
                    try { l.onThemeChanged(colors); }
                    catch (Exception e) {
                        System.err.println("[ThemeManager] Listener update failed: " + e.getMessage());
                    }
                }
            }
        });
    }

    public void applyTheme(String themeName) {
        try {
            applyTheme(Theme.valueOf(themeName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            System.err.println("[ThemeManager] Unknown theme '" + themeName + "', defaulting to LIGHT.");
            applyTheme(Theme.LIGHT);
        }
    }

    public ThemeColors currentColors() {
        return getColors(currentTheme);
    }

    public Theme getCurrentTheme() { return currentTheme; }

    public String getCurrentThemeName() { return currentTheme.name(); }

    // Maps enum → palette
    public static ThemeColors getColors(Theme t) {
        switch (t) {
            case DARK:  return DARK;
            case BLUE:  return BLUE;
            case GREEN: return GREEN;
            default:    return LIGHT;
        }
    }

    public static ThemeColors getColors(String name) {
        try { return getColors(Theme.valueOf(name.toUpperCase())); }
        catch (IllegalArgumentException e) { return LIGHT; }
    }
}