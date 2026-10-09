package main.database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import main.exceptions.DatabaseException;
import main.exceptions.GameException;   // <-- added import

/**
 * HistoryDAO – Feature 3a / Feature 5.
 *
 * Provides paginated, hierarchical access to:
 *   Session  →  Game  →  Move
 *
 * All data classes are plain value objects (no business logic).
 */
public class HistoryDAO {

    // ── Data classes ──────────────────────────────────────────────────────────

    public static class SessionRow {
        public final int           id;
        public final LocalDateTime startedAt;
        public final LocalDateTime endedAt;
        public final String        status;
        public final int           boardSize;
        public final String        gamemode;
        public final String        difficulty;
        public final String        winningLogic;
        public final String        player1Name;
        public final String        player2Name;

        SessionRow(int id, LocalDateTime sa, LocalDateTime ea, String status,
                   int bs, String mode, String diff, String wl, String p1, String p2) {
            this.id = id; startedAt = sa; endedAt = ea; this.status = status;
            boardSize = bs; gamemode = mode; difficulty = diff;
            winningLogic = wl; player1Name = p1; player2Name = p2;
        }
    }

    public static class GameRow {
        public final int           id;
        public final int           sessionId;
        public final LocalDateTime startedAt;
        public final LocalDateTime endedAt;
        public final String        result;
        public final int           durationSec;
        public final int           moveCount;

        GameRow(int id, int sid, LocalDateTime sa, LocalDateTime ea,
                String result, int dur, int moves) {
            this.id = id; sessionId = sid; startedAt = sa; endedAt = ea;
            this.result = result; durationSec = dur; moveCount = moves;
        }
    }

    public static class MoveRow {
        public final int           id;
        public final int           gameId;
        public final int           moveNumber;
        public final String        player;
        public final int           row;
        public final int           col;
        public final LocalDateTime movedAt;

        MoveRow(int id, int gid, int mn, String pl, int r, int c, LocalDateTime ma) {
            this.id = id; gameId = gid; moveNumber = mn; player = pl;
            row = r; col = c; movedAt = ma;
        }
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    /**
     * Returns sessions ordered newest-first, with pagination support.
     * @param page      0-based page index
     * @param pageSize  rows per page (e.g. 20)
     */
    public static List<SessionRow> getSessions(int page, int pageSize) throws DatabaseException {
        String sql =
            "SELECT id, started_at, ended_at, status, board_size, gamemode, " +
            "       difficulty, winning_logic, player1_name, player2_name " +
            "FROM sessions ORDER BY started_at DESC LIMIT ? OFFSET ?";

        List<SessionRow> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, page * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ea = rs.getTimestamp("ended_at");
                    rows.add(new SessionRow(
                        rs.getInt("id"),
                        rs.getTimestamp("started_at").toLocalDateTime(),
                        ea != null ? ea.toLocalDateTime() : null,
                        rs.getString("status"),
                        rs.getInt("board_size"),
                        rs.getString("gamemode"),
                        rs.getString("difficulty"),
                        rs.getString("winning_logic"),
                        rs.getString("player1_name"),
                        rs.getString("player2_name")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load sessions.", e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {   // <-- handle connection failure
            throw new DatabaseException("Database connection error while loading sessions.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
        return rows;
    }

    /** Returns total session count (for paging controls). */
    public static int getSessionCount() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM sessions";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count sessions.", e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while counting sessions.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
    }

    /** Returns all games for a given session. */
    public static List<GameRow> getGamesForSession(int sessionId) throws DatabaseException {
        String sql =
            "SELECT id, session_id, started_at, ended_at, result, duration_sec, move_count " +
            "FROM games WHERE session_id = ? ORDER BY started_at ASC";

        List<GameRow> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ea = rs.getTimestamp("ended_at");
                    rows.add(new GameRow(
                        rs.getInt("id"),
                        rs.getInt("session_id"),
                        rs.getTimestamp("started_at").toLocalDateTime(),
                        ea != null ? ea.toLocalDateTime() : null,
                        rs.getString("result"),
                        rs.getInt("duration_sec"),
                        rs.getInt("move_count")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load games for session " + sessionId, e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while loading games for session.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
        return rows;
    }

    /** Returns all moves for a given game, ordered by move_number. */
    public static List<MoveRow> getMovesForGame(int gameId) throws DatabaseException {
        String sql =
            "SELECT id, game_id, move_number, player, row_pos, col_pos, moved_at " +
            "FROM moves WHERE game_id = ? ORDER BY move_number ASC";

        List<MoveRow> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new MoveRow(
                        rs.getInt("id"),
                        rs.getInt("game_id"),
                        rs.getInt("move_number"),
                        rs.getString("player"),
                        rs.getInt("row_pos"),
                        rs.getInt("col_pos"),
                        rs.getTimestamp("moved_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load moves for game " + gameId, e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while loading moves.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
        return rows;
    }

    // ── Deletion ──────────────────────────────────────────────────────────────

    /** Deletes ALL history (sessions, games, moves) – Feature 3f. */
    public static void clearAllHistory() throws DatabaseException {
        String sql = "DELETE FROM sessions";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to clear history.", e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while clearing history.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
    }

    /** Deletes a single session (and cascades to its games/moves). */
    public static void deleteSession(int sessionId) throws DatabaseException {
        String sql = "DELETE FROM sessions WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete session " + sessionId, e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while deleting session.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
    }
}