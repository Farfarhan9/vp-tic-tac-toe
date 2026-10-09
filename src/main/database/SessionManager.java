package main.database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import main.exceptions.DatabaseException;
import main.exceptions.GameException;

/**
 * SessionManager – Feature 2: Session persistence.
 *
 * Sessions remain 'active' until explicitly abandoned (user chooses "Start New").
 * This allows multiple games within a single session (e.g., rematches).
 */
public class SessionManager {

    // ── Data transfer objects ─────────────────────────────────────────────────

    /** Lightweight summary of a session used for the "continue?" dialog. */
    public static class SessionSummary {
        public final int    sessionId;
        public final int    gameId;
        public final int    boardSize;
        public final String gamemode;
        public final String difficulty;
        public final String winningLogic;
        public final String firstPlayer;
        public final String player1Name;
        public final String player2Name;
        public final LocalDateTime startedAt;
        public final int    moveCount;
        public final int    humanWins;
        public final int    botWins;
        public final int    draws;
        /** The result of the latest game: "player1", "player2", "draw", or null (if unfinished). */
        public final String gameResult;
        /** Symbol ("X" or "O") of the player whose turn is next. Valid only if gameResult == null. */
        public final String nextPlayer;

        SessionSummary(int sid, int gid, int bs, String mode, String diff,
                       String wl, String fp, String p1, String p2,
                       LocalDateTime sa, int mc, int hw, int bw, int d,
                       String gResult, String nextP) {
            sessionId    = sid;  gameId      = gid;  boardSize  = bs;
            gamemode     = mode; difficulty  = diff; winningLogic = wl;
            firstPlayer  = fp;   player1Name = p1;   player2Name = p2;
            startedAt    = sa;   moveCount   = mc;
            humanWins    = hw;   botWins     = bw;   draws       = d;
            gameResult   = gResult;
            nextPlayer   = nextP;
        }
    }

    /** A single recorded move, used to restore board state. */
    public static class MoveRecord {
        public final int    moveNumber;
        public final String player;
        public final int    row;
        public final int    col;
        MoveRecord(int mn, String pl, int r, int c) {
            moveNumber = mn; player = pl; row = r; col = c;
        }
    }

    // ── Session lifecycle ─────────────────────────────────────────────────────

    /**
     * Returns the most recent active session (even if its last game is finished).
     * The session remains active until the user abandons it.
     */
    public static SessionSummary findActiveSession() throws DatabaseException {
        // Query that returns the most recent active session and its latest game.
        String sql =
            "SELECT s.id, s.board_size, s.gamemode, s.difficulty, " +
            "       s.winning_logic, s.first_player, s.player1_name, s.player2_name, " +
            "       s.started_at, s.human_wins, s.bot_wins, s.draws, " +
            "       g.id AS gid, g.move_count, g.result AS game_result " +
            "FROM sessions s " +
            "JOIN games g ON g.session_id = s.id " +
            "WHERE s.status = 'active' " +
            "ORDER BY s.started_at DESC, g.started_at DESC LIMIT 1";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int gameId = rs.getInt("gid");
                String firstPlayer = rs.getString("first_player");
                String gameResult = rs.getString("game_result");
                String nextPlayer = null;
                // Only derive next player if the game is unfinished
                if (gameResult == null) {
                    nextPlayer = deriveNextPlayer(gameId, firstPlayer);
                }
                return new SessionSummary(
                    rs.getInt("id"),
                    gameId,
                    rs.getInt("board_size"),
                    rs.getString("gamemode"),
                    rs.getString("difficulty"),
                    rs.getString("winning_logic"),
                    firstPlayer,
                    rs.getString("player1_name"),
                    rs.getString("player2_name"),
                    rs.getTimestamp("started_at").toLocalDateTime(),
                    rs.getInt("move_count"),
                    rs.getInt("human_wins"),
                    rs.getInt("bot_wins"),
                    rs.getInt("draws"),
                    gameResult,
                    nextPlayer
                );
            }
        } catch (SQLException | GameException e) {
            System.err.println("[SessionManager] findActiveSession query failed: " + e.getMessage());
            // Fallback: try a simpler query if the join fails
            return findActiveSessionFallback();
        }
        return null;
    }

    // ★ Fallback method in case the main query fails.
    private static SessionSummary findActiveSessionFallback() throws DatabaseException {
        String sessionSql =
            "SELECT id, board_size, gamemode, difficulty, winning_logic, " +
            "       first_player, player1_name, player2_name, started_at, " +
            "       human_wins, bot_wins, draws " +
            "FROM sessions WHERE status = 'active' ORDER BY started_at DESC LIMIT 1";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sessionSql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int sessionId = rs.getInt("id");
                String firstPlayer = rs.getString("first_player");
                // Get the latest game for this session
                String gameSql =
                    "SELECT id, move_count, result FROM games WHERE session_id = ? " +
                    "ORDER BY started_at DESC LIMIT 1";
                try (PreparedStatement ps2 = c.prepareStatement(gameSql)) {
                    ps2.setInt(1, sessionId);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        if (rs2.next()) {
                            int gameId = rs2.getInt("id");
                            String gameResult = rs2.getString("result");
                            String nextPlayer = null;
                            if (gameResult == null) {
                                nextPlayer = deriveNextPlayer(gameId, firstPlayer);
                            }
                            return new SessionSummary(
                                sessionId,
                                gameId,
                                rs.getInt("board_size"),
                                rs.getString("gamemode"),
                                rs.getString("difficulty"),
                                rs.getString("winning_logic"),
                                firstPlayer,
                                rs.getString("player1_name"),
                                rs.getString("player2_name"),
                                rs.getTimestamp("started_at").toLocalDateTime(),
                                rs2.getInt("move_count"),
                                rs.getInt("human_wins"),
                                rs.getInt("bot_wins"),
                                rs.getInt("draws"),
                                gameResult,
                                nextPlayer
                            );
                        }
                    }
                }
            }
        } catch (SQLException | GameException e) {
            System.err.println("[SessionManager] Fallback failed: " + e.getMessage());
            throw new DatabaseException("Could not check for active session.", e,
                    DatabaseException.ErrorSeverity.WARNING);
        }
        return null;
    }

    /**
     * Figures out whose turn it is next by looking at the last move for this game.
     * If no moves recorded yet, it's firstPlayer's turn.
     */
    private static String deriveNextPlayer(int gameId, String firstPlayer)
            throws DatabaseException {
        String sql = "SELECT player FROM moves WHERE game_id = ? ORDER BY move_number DESC LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return firstPlayer;  // no moves yet
                String lastPlayer = rs.getString("player");
                // Next = the other player
                return lastPlayer.equals("X") ? "O" : "X";
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not derive next player.", e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while deriving next player.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
    }

    /**
     * Creates a brand-new session and its first game.
     * @return the new game id
     */
    public static int createNewSession(int boardSize, String gamemode, String difficulty,
                                       String winningLogic, String firstPlayer,
                                       String player1Name, String player2Name)
            throws DatabaseException {

        String insertSession =
            "INSERT INTO sessions (board_size, gamemode, difficulty, winning_logic, " +
            "                      first_player, player1_name, player2_name, status, " +
            "                      human_wins, bot_wins, draws) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, 'active', 0, 0, 0)";

        String insertGame =
            "INSERT INTO games (session_id) VALUES (?)";

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int sessionId;
                try (PreparedStatement ps = c.prepareStatement(insertSession,
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, boardSize);
                    ps.setString(2, gamemode);
                    ps.setString(3, difficulty);
                    ps.setString(4, winningLogic);
                    ps.setString(5, firstPlayer);
                    ps.setString(6, player1Name);
                    ps.setString(7, player2Name);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        sessionId = keys.getInt(1);
                    }
                }

                int gameId;
                try (PreparedStatement ps2 = c.prepareStatement(insertGame,
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps2.setInt(1, sessionId);
                    ps2.executeUpdate();
                    try (ResultSet keys = ps2.getGeneratedKeys()) {
                        keys.next();
                        gameId = keys.getInt(1);
                    }
                }

                c.commit();
                return gameId;

            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create new session.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while creating session.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
    }

    /**
     * Adds a new game to an existing session.
     * @param sessionId the session to add the game to
     * @return the new game id
     */
    public static int createNewGame(int sessionId) throws DatabaseException {
        String insertGame = "INSERT INTO games (session_id) VALUES (?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(insertGame,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create new game for session " + sessionId, e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while creating new game.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
    }

    /**
     * Marks the session as abandoned (user chose "Start New").
     */
    public static void abandonSession(int sessionId) throws DatabaseException {
        String sql = "UPDATE sessions SET status = 'abandoned', ended_at = NOW() WHERE id = ?";
        String sqlGames = "UPDATE games SET result = 'abandoned', ended_at = NOW() " +
                          "WHERE session_id = ? AND result IS NULL";

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps  = c.prepareStatement(sql);
                 PreparedStatement ps2 = c.prepareStatement(sqlGames)) {
                ps.setInt(1, sessionId);  ps.executeUpdate();
                ps2.setInt(1, sessionId); ps2.executeUpdate();
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to abandon session " + sessionId, e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while abandoning session.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
    }

    /**
     * Marks a game as complete. The session remains 'active' so that
     * the user can continue with a new game in the same session.
     */
    public static void completeGame(int gameId, String result, int durationSec, int moveCount,
                                    int humanWins, int botWins, int draws)
            throws DatabaseException {

        String sqlGame = "UPDATE games SET result = ?, ended_at = NOW(), " +
                         "duration_sec = ?, move_count = ? WHERE id = ?";
        String sqlSession = "UPDATE sessions SET human_wins = ?, bot_wins = ?, draws = ? " +
                            "WHERE id = (SELECT session_id FROM games WHERE id = ?)";

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps  = c.prepareStatement(sqlGame);
                 PreparedStatement ps2 = c.prepareStatement(sqlSession)) {
                ps.setString(1, result);
                ps.setInt(2, durationSec);
                ps.setInt(3, moveCount);
                ps.setInt(4, gameId);
                ps.executeUpdate();

                ps2.setInt(1, humanWins);
                ps2.setInt(2, botWins);
                ps2.setInt(3, draws);
                ps2.setInt(4, gameId);
                ps2.executeUpdate();

                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to complete game " + gameId, e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while completing game.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
    }

    // ── Timer and move helpers (unchanged) ────────────────────────────────────

    public static void saveGameDuration(int gameId, int durationSec) throws DatabaseException {
        String sql = "UPDATE games SET duration_sec = ? WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, durationSec);
            ps.setInt(2, gameId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save game duration.", e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while saving game duration.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
    }

    public static void recordMove(int gameId, int moveNumber, String player, int row, int col)
            throws DatabaseException {
        String sql = "INSERT INTO moves (game_id, move_number, player, row_pos, col_pos) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setInt(2, moveNumber);
            ps.setString(3, player);
            ps.setInt(4, row);
            ps.setInt(5, col);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to record move.", e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while recording move.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
    }

    public static List<MoveRecord> loadMoves(int gameId) throws DatabaseException {
        String sql = "SELECT move_number, player, row_pos, col_pos FROM moves " +
                     "WHERE game_id = ? ORDER BY move_number ASC";
        List<MoveRecord> records = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new MoveRecord(
                        rs.getInt("move_number"),
                        rs.getString("player"),
                        rs.getInt("row_pos"),
                        rs.getInt("col_pos")
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
        return records;
    }

    public static int loadGameDuration(int gameId) throws DatabaseException {
        String sql = "SELECT duration_sec FROM games WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("duration_sec");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load duration for game " + gameId, e,
                    DatabaseException.ErrorSeverity.WARNING);
        } catch (GameException e) {
            throw new DatabaseException("Database connection error while loading duration.", e,
                    DatabaseException.ErrorSeverity.CRITICAL);
        }
        return 0;
    }
}