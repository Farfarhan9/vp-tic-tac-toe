package main.game;

/**
 * Fully merged GameModel.
 * Combines original code with Claude's v2 extensions.
 * All existing behaviour is preserved; new features are additive.
 */
public class GameModel {

    // ── Board state ───────────────────────────────────────────────────────────
    private String[][] board;
    private int        boardSize      = 3;
    private int        moveCount      = 0;
    private boolean    gameOver       = false;
    private String     currentPlayer;

    // ── Player symbols ────────────────────────────────────────────────────────
    private String player1Symbol = "X";   // human / P1
    private String player2Symbol = "O";   // bot   / P2

    // ── Score counters ────────────────────────────────────────────────────────
    private int humanWins = 0;
    private int botWins   = 0;
    private int draws     = 0;

    // ── Game settings ─────────────────────────────────────────────────────────
    private String  gameMode            = "Singleplayer";
    private boolean matchTimerEnabled   = true;
    private boolean boardInfoEnabled    = true;
    private boolean playerCounterEnabled = true;

    // ── v2: new settings fields ──────────────────────────────────────────────
    private String firstPlayer  = "X";
    private String theme        = "Light";
    private String musicTrack   = "background.wav";
    private String xImagePath   = null;
    private String oImagePath   = null;
    private String difficulty   = "Medium";
    private String winningLogic = "Default";
    private int currentGameId    = -1;
    private int currentSessionId = -1;

    // ── Constructor ───────────────────────────────────────────────────────────
    public GameModel() {
        this.boardSize = 3;
        this.gameMode = "Singleplayer";
        this.player1Symbol = "X";
        this.player2Symbol = "O";
        this.matchTimerEnabled = true;
        this.boardInfoEnabled = true;
        this.playerCounterEnabled = true;
        resetGame();
    }

    // ── Core game operations ──────────────────────────────────────────────────

    public synchronized void resetGame() {
        board = new String[boardSize][boardSize];
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                board[r][c] = "";
            }
        }
        moveCount = 0;
        gameOver = false;
        currentPlayer = firstPlayer;
    }

    /** ★ NEW: Reset all win counters to zero. */
    public synchronized void resetScores() {
        humanWins = 0;
        botWins   = 0;
        draws     = 0;
    }

    public synchronized boolean setMove(int row, int col) {
        if (row < 0 || row >= boardSize || col < 0 || col >= boardSize ||
            !board[row][col].isEmpty() || gameOver) {
            return false;
        }
        board[row][col] = currentPlayer;
        moveCount++;
        return true;
    }

    public synchronized int[][] checkWinCoordinates() {
        int winLen = winningLogic.equals("3x3 Rules") ? 3 : boardSize;
        return checkWinCoordinatesWithLength(winLen);
    }

    private int[][] checkWinCoordinatesWithLength(int winLen) {
        for (int r = 0; r < boardSize; r++) {
            for (int cStart = 0; cStart <= boardSize - winLen; cStart++) {
                int[][] line = checkLine(r, cStart, 0, 1, winLen);
                if (line != null) return line;
            }
        }
        for (int c = 0; c < boardSize; c++) {
            for (int rStart = 0; rStart <= boardSize - winLen; rStart++) {
                int[][] line = checkLine(rStart, c, 1, 0, winLen);
                if (line != null) return line;
            }
        }
        for (int r = 0; r <= boardSize - winLen; r++) {
            for (int c = 0; c <= boardSize - winLen; c++) {
                int[][] line = checkLine(r, c, 1, 1, winLen);
                if (line != null) return line;
            }
        }
        for (int r = 0; r <= boardSize - winLen; r++) {
            for (int c = winLen - 1; c < boardSize; c++) {
                int[][] line = checkLine(r, c, 1, -1, winLen);
                if (line != null) return line;
            }
        }
        return null;
    }

    private int[][] checkLine(int startR, int startC, int dr, int dc, int len) {
        String first = board[startR][startC];
        if (first == null || first.isEmpty()) return null;
        int[][] coords = new int[len][2];
        coords[0] = new int[]{startR, startC};
        for (int i = 1; i < len; i++) {
            int r = startR + i * dr;
            int c = startC + i * dc;
            if (!board[r][c].equals(first)) return null;
            coords[i] = new int[]{r, c};
        }
        return coords;
    }

    public synchronized boolean checkDraw() {
        if (gameOver) return false;
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (board[r][c].isEmpty()) return false;
            }
        }
        return true;
    }

    public synchronized void switchPlayer() {
        if (currentPlayer.equals(player1Symbol)) {
            currentPlayer = player2Symbol;
        } else {
            currentPlayer = player1Symbol;
        }
    }

    // ── Getters / Setters (existing) ──────────────────────────────────────────

    public synchronized int getBoardSize()          { return boardSize; }
    public synchronized String getCurrentPlayer()   { return currentPlayer; }
    public synchronized int getMoveCount()          { return moveCount; }
    public synchronized int getHumanWins()          { return humanWins; }
    public synchronized int getBotWins()            { return botWins; }
    public synchronized int getDraws()              { return draws; }
    public synchronized boolean isGameOver()        { return gameOver; }
    public synchronized String getGameMode()        { return gameMode; }
    public synchronized String getPlayer1Symbol()   { return player1Symbol; }
    public synchronized String getPlayer2Symbol()   { return player2Symbol; }
    public synchronized String[][] getBoardState()  { return board; }

    public synchronized boolean isMatchTimerEnabled()     { return matchTimerEnabled; }
    public synchronized boolean isBoardInfoEnabled()      { return boardInfoEnabled; }
    public synchronized boolean isPlayerCounterEnabled()  { return playerCounterEnabled; }

    public synchronized void setGameOver(boolean gameOver) { this.gameOver = gameOver; }
    public synchronized void incrementHumanWins()          { humanWins++; }
    public synchronized void incrementBotWins()            { botWins++; }
    public synchronized void incrementDraws()              { draws++; }
    public synchronized void setGameMode(String gameMode)  { this.gameMode = gameMode; resetGame(); }
    public synchronized void setMatchTimerEnabled(boolean v)  { matchTimerEnabled = v; }
    public synchronized void setBoardInfoEnabled(boolean v)   { boardInfoEnabled = v; }
    public synchronized void setPlayerCounterEnabled(boolean v){ playerCounterEnabled = v; }

    public synchronized void setBoardSize(int boardSize) {
        if (boardSize < 3 || boardSize > 6) return;
        this.boardSize = boardSize;
        resetGame();
    }

    // ── Getters / Setters (v2 new fields) ────────────────────────────────────

    public synchronized String getFirstPlayer()          { return firstPlayer; }
    public synchronized void setFirstPlayer(String fp)   {
        if (fp != null && (fp.equals("X") || fp.equals("O"))) {
            firstPlayer = fp;
            currentPlayer = firstPlayer;
        }
    }

    public synchronized String getTheme()                { return theme; }
    public synchronized void setTheme(String theme)     { this.theme = (theme != null) ? theme : "Light"; }

    public synchronized String getMusicTrack()           { return musicTrack; }
    public synchronized void setMusicTrack(String track){ this.musicTrack = (track != null) ? track : "background.wav"; }

    public synchronized String getXImagePath()           { return xImagePath; }
    public synchronized void setXImagePath(String path) { this.xImagePath = path; }

    public synchronized String getOImagePath()           { return oImagePath; }
    public synchronized void setOImagePath(String path) { this.oImagePath = path; }

    public synchronized String getDifficulty()           { return difficulty; }
    public synchronized void setDifficulty(String diff) 
    {
    	System.out.println("GameModel.setDifficulty() called: " + difficulty + " (previous: " + this.difficulty + ")"); this.difficulty = (diff != null) ? diff : "Medium";
    	new Exception("Stack trace").printStackTrace(System.out); 
    	}

    public synchronized String getWinningLogic()         { return winningLogic; }
    public synchronized void setWinningLogic(String wl) { this.winningLogic = (wl != null) ? wl : "Default"; }

    public synchronized int getCurrentGameId()           { return currentGameId; }
    public synchronized void setCurrentGameId(int id)   { this.currentGameId = id; }

    public synchronized int getCurrentSessionId()        { return currentSessionId; }
    public synchronized void setCurrentSessionId(int id){ this.currentSessionId = id; }

    // ── Session restoration helpers ──────────────────────────────────────────

    public synchronized void setBoardCell(int row, int col, String symbol) {
        if (row >= 0 && row < boardSize && col >= 0 && col < boardSize) {
            board[row][col] = symbol;
        }
    }

    public synchronized void incrementMoveCount() {
        moveCount++;
    }

    public synchronized void setCurrentPlayer(String player) {
        if (player != null && (player.equals(player1Symbol) || player.equals(player2Symbol))) {
            this.currentPlayer = player;
        }
    }
    public synchronized void setHumanWins(int wins) { this.humanWins = wins; }
    public synchronized void setBotWins(int wins)   { this.botWins = wins; }
    public synchronized void setDraws(int draws)    { this.draws = draws; }
}