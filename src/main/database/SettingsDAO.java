package main.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import main.game.GameModel;
import main.exceptions.GameException;

/**
 * SettingsDAO – v3 update.
 *
 * Added persistence for:
 *   • difficulty         (so that the "Impossible" setting is not lost)
 *   • first_player
 *   • theme
 *   • music_track
 *   • x_image / o_image
 *
 * Also provides helpers for the music library.
 */
public class SettingsDAO {

    // ── Settings CRUD ─────────────────────────────────────────────────────────

    /**
     * Persist all settings (including difficulty) from the GameModel into DB.
     */
    public static void saveSettings(GameModel model) throws GameException {
        String query =
            "UPDATE game_settings SET " +
            "  gamemode = ?, board_size = ?, match_timer = ?, " +
            "  board_info = ?, player_counter = ?, first_player = ?, " +
            "  theme = ?, music_track = ?, x_image = ?, o_image = ?, " +
            "  difficulty = ? " +
            "WHERE id = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1,  model.getGameMode());
            ps.setInt(2,     model.getBoardSize());
            ps.setBoolean(3, model.isMatchTimerEnabled());
            ps.setBoolean(4, model.isBoardInfoEnabled());
            ps.setBoolean(5, model.isPlayerCounterEnabled());
            ps.setString(6,  model.getFirstPlayer());
            ps.setString(7,  model.getTheme());
            ps.setString(8,  model.getMusicTrack());
            ps.setString(9,  model.getXImagePath());
            ps.setString(10, model.getOImagePath());
            ps.setString(11, model.getDifficulty());   // ★ NEW

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated == 0) {
                // No row with id=1 → insert
                String insertQuery =
                    "INSERT INTO game_settings " +
                    "  (id, gamemode, board_size, match_timer, board_info, player_counter, " +
                    "   first_player, theme, music_track, x_image, o_image, difficulty) " +
                    "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psInsert = conn.prepareStatement(insertQuery)) {
                    psInsert.setString(1,  model.getGameMode());
                    psInsert.setInt(2,     model.getBoardSize());
                    psInsert.setBoolean(3, model.isMatchTimerEnabled());
                    psInsert.setBoolean(4, model.isBoardInfoEnabled());
                    psInsert.setBoolean(5, model.isPlayerCounterEnabled());
                    psInsert.setString(6,  model.getFirstPlayer());
                    psInsert.setString(7,  model.getTheme());
                    psInsert.setString(8,  model.getMusicTrack());
                    psInsert.setString(9,  model.getXImagePath());
                    psInsert.setString(10, model.getOImagePath());
                    psInsert.setString(11, model.getDifficulty());   // ★ NEW
                    psInsert.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new GameException("Persistent layer write operation halted writing settings.", e,
                    GameException.ErrorSeverity.WARNING);
        }
    }

    /**
     * Load all settings (including difficulty) from the DB into the GameModel.
     */
    public static void loadSettings(GameModel model) throws GameException {
        String query =
            "SELECT gamemode, board_size, match_timer, board_info, player_counter, " +
            "       first_player, theme, music_track, x_image, o_image, difficulty " +  // ★ ADD difficulty
            "FROM game_settings WHERE id = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                model.setGameMode(rs.getString("gamemode"));
                model.setBoardSize(rs.getInt("board_size"));
                model.setMatchTimerEnabled(rs.getBoolean("match_timer"));
                model.setBoardInfoEnabled(rs.getBoolean("board_info"));
                model.setPlayerCounterEnabled(rs.getBoolean("player_counter"));

                // v2/v3 fields – guard against missing columns (if schema not updated)
                try { model.setFirstPlayer(rs.getString("first_player")); }
                catch (SQLException ignored) {}
                try { model.setTheme(rs.getString("theme")); }
                catch (SQLException ignored) {}
                try { model.setMusicTrack(rs.getString("music_track")); }
                catch (SQLException ignored) {}
                try { model.setXImagePath(rs.getString("x_image")); }
                catch (SQLException ignored) {}
                try { model.setOImagePath(rs.getString("o_image")); }
                catch (SQLException ignored) {}
                try { model.setDifficulty(rs.getString("difficulty")); }   // ★ NEW
                catch (SQLException ignored) {}

            } else {
                throw new GameException(
                    "Initial configuration record missing. Using defaults.",
                    GameException.ErrorSeverity.INFO);
            }
        } catch (SQLException e) {
            throw new GameException(
                "Configuration extraction failed. Using defaults.", e,
                GameException.ErrorSeverity.INFO);
        }
    }

    // ── Music library helpers (Feature 3c) ────────────────────────────────────

    /** Simple value object for a music track entry. */
    public static class MusicTrack {
        public final int    id;
        public final String name;
        public final String filename;
        public MusicTrack(int id, String name, String filename) {
            this.id = id; this.name = name; this.filename = filename;
        }
        @Override public String toString() { return name; }
    }

    /** Returns all tracks from the music_library table. */
    public static List<MusicTrack> getAllTracks() throws GameException {
        String sql = "SELECT id, name, filename FROM music_library ORDER BY added_at ASC";
        List<MusicTrack> tracks = new ArrayList<>();
        try (Connection c  = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tracks.add(new MusicTrack(rs.getInt("id"),
                        rs.getString("name"), rs.getString("filename")));
            }
        } catch (SQLException e) {
            throw new GameException("Failed to load music library.", e,
                    GameException.ErrorSeverity.WARNING);
        }
        return tracks;
    }

    /** Adds a track to the library. */
    public static void addTrack(String name, String filename) throws GameException {
        String sql = "INSERT INTO music_library (name, filename) VALUES (?, ?)";
        try (Connection c  = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, filename);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new GameException("Failed to add music track.", e,
                    GameException.ErrorSeverity.WARNING);
        }
    }
}